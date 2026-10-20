package com.fit15.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val ScreenPad = 20.dp

@Composable
private fun Rule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun SignalButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Signal, contentColor = Ink),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) { Text(text, style = Type.section) }
}

@Composable
private fun BackLink(onBack: () -> Unit) {
    Text(
        "‹ Back",
        style = Type.row,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onBack).padding(vertical = 14.dp),
    )
}

@Composable
fun HomeScreen(
    store: Store,
    history: List<Store.Entry>,
    programDone: Int,
    update: UpdateInfo?,
    onOpen: (Workout) -> Unit,
    onProgram: () -> Unit,
    onRestartProgram: () -> Unit,
    onMuscles: () -> Unit,
) {
    val streak = streakOf(history)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = ScreenPad, end = ScreenPad, top = 8.dp, bottom = 32.dp)) {
        if (update != null) item { UpdateBanner(update) }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
                Text("Fit15", style = Type.title, color = accentText(), modifier = Modifier.weight(1f))
                Text("$streak", style = Type.number)
                Text(" day streak", style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        item { Hero(programDone, onProgram, onRestartProgram) }
        item { WeekStrip(history) }
        item { Spacer(Modifier.height(8.dp)); Rule(); MusclesRow(history, onMuscles); Rule() }
        item { SectionTitle("Workouts") }
        items(Workouts.main) { w -> WorkoutRow(w) { onOpen(w) } }
        item { SectionTitle("When you only have seven minutes") }
        items(Workouts.quick) { w -> WorkoutRow(w) { onOpen(w) } }
        item { Spacer(Modifier.height(24.dp)); ReminderRow(store) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = Type.section, modifier = Modifier.padding(top = 28.dp, bottom = 6.dp))
    Rule()
}

@Composable
private fun UpdateBanner(info: UpdateInfo) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).background(Ink).padding(16.dp),
    ) {
        Text("Version ${info.version} is ready", style = Type.section, color = Chalk)
        if (info.notes.isNotBlank()) {
            Text(info.notes, style = Type.small, maxLines = 3, color = Chalk.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
        }
        msg?.let { Text(it, style = Type.small, color = Chalk, modifier = Modifier.padding(top = 6.dp)) }
        Button(
            enabled = progress == null,
            onClick = {
                scope.launch {
                    progress = 0f
                    msg = null
                    try {
                        val file = withContext(Dispatchers.IO) { downloadUpdate(ctx, info) { progress = it } }
                        progress = null
                        if (!installApk(ctx, file)) msg = "Allow installing from Fit15 in the settings that just opened, then tap Update again."
                    } catch (e: Exception) {
                        progress = null
                        msg = "The update did not download: ${e.message}"
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Signal, contentColor = Ink),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            val p = progress
            Text(if (p != null) "Downloading ${(p * 100).toInt()}%" else "Update", fontWeight = FontWeight.Bold)
        }
    }
}

/** Het ene grote blok: de training van vandaag. */
@Composable
private fun Hero(done: Int, onStart: () -> Unit, onRestart: () -> Unit) {
    val next = Program.next(done)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 28.dp, bottomEnd = 4.dp, bottomStart = 28.dp))
            .background(Ink)
            .padding(22.dp),
    ) {
        Text(
            if (next != null) "Week ${next.weekIndex + 1}, ${next.week.title}" else Program.NAME,
            style = Type.small,
            color = Chalk.copy(alpha = 0.7f),
        )
        Text(
            next?.workout?.name ?: "Program complete",
            style = Type.hero,
            color = Chalk,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            if (next != null) "${next.week.intensity.workSec} seconds on, ${next.week.intensity.restSec} off. ${next.workout.minutes} minutes."
            else "All ${Program.total} sessions are done. Go again with slower reps.",
            style = Type.body,
            color = Chalk.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(Program.total) { i ->
                Box(
                    Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i < done) Signal else Chalk.copy(alpha = 0.2f)),
                )
            }
        }
        Text("$done of ${Program.total} sessions", style = Type.small, color = Chalk.copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp))
        SignalButton(if (next != null) "Start session" else "Start again", Modifier.padding(top = 16.dp), if (next != null) onStart else onRestart)
    }
}

@Composable
private fun WeekStrip(history: List<Store.Entry>) {
    val days = history.map { it.day }.toSet()
    val today = LocalDate.now()
    val total = history.size
    val minutes = history.sumOf { it.minutes }
    Column(Modifier.padding(top = 18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 6 downTo 0) {
                val d = today.minusDays(i.toLong())
                val done = d.toEpochDay() in days
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        style = Type.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (done) Signal else Color.Transparent)
                            .border(if (i == 0) 2.dp else 1.dp, if (done) Signal else if (i == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (done) "✓" else d.dayOfMonth.toString(),
                            style = Type.small,
                            fontWeight = FontWeight.Bold,
                            color = if (done) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            if (total == 0) "No workouts yet. Your first one lights up the week." else "$total workouts and $minutes minutes in total.",
            style = Type.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun MusclesRow(history: List<Store.Entry>, onClick: () -> Unit) {
    val sets = weeklySets(history)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        BodyMap(sets, height = 110.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Your muscles", style = Type.section)
            Text(muscleSummary(sets), style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkoutRow(w: Workout, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(w.name, style = Type.row)
            Text(w.focus, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${w.minutes}", style = Type.number)
            Text("min", style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Rule()
}

@Composable
private fun ReminderRow(store: Store) {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(store.reminderOn) }
    var hour by remember { mutableIntStateOf(store.reminderHour) }
    var minute by remember { mutableIntStateOf(store.reminderMinute) }
    var blocked by remember { mutableStateOf(false) }

    fun enable() {
        store.reminderOn = true
        on = true
        Reminder.schedule(ctx, hour, minute)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            blocked = false
            enable()
        } else {
            blocked = true
        }
    }

    Column {
        Rule()
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily reminder", style = Type.row)
                Text(
                    "%02d:%02d, tap to change".format(hour, minute),
                    style = Type.small,
                    color = accentText(),
                    modifier = Modifier.clickable {
                        TimePickerDialog(ctx, { _, h, m ->
                            hour = h
                            minute = m
                            store.reminderHour = h
                            store.reminderMinute = m
                            if (on) Reminder.schedule(ctx, h, m)
                        }, hour, minute, true).show()
                    },
                )
            }
            Switch(
                checked = on,
                colors = SwitchDefaults.colors(checkedTrackColor = Signal, checkedThumbColor = Ink),
                onCheckedChange = { want ->
                    if (want) {
                        val needsAsk = Build.VERSION.SDK_INT >= 33 &&
                            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsAsk) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else enable()
                    } else {
                        store.reminderOn = false
                        on = false
                        Reminder.cancel(ctx)
                    }
                },
            )
        }
        if (blocked) {
            Text(
                "Notifications are blocked for Fit15. Allow them in your phone settings to get reminders.",
                style = Type.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Rule()
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (i == selected) Signal else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Type.row, color = if (i == selected) Ink else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun DetailScreen(
    workout: Workout,
    level: Level,
    onLevel: (Level) -> Unit,
    program: Program.Next?,
    warmUp: Boolean,
    onWarmUp: (Boolean) -> Unit,
    onBack: () -> Unit,
    onStart: (Intensity) -> Unit,
) {
    val intensity = program?.week?.intensity ?: level.intensity
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = ScreenPad)) {
            item {
                BackLink(onBack)
                Text(workout.name, style = Type.title)
                Text(workout.focus, style = Type.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                if (program != null) {
                    Text("Week ${program.weekIndex + 1}, ${program.week.title}", style = Type.row)
                } else {
                    Segmented(Level.entries.map { it.label }, Level.entries.indexOf(level)) { onLevel(Level.entries[it]) }
                }
                Text(
                    "${intensity.workSec} seconds on, ${intensity.restSec} off. " +
                        "${workout.exercises.size} exercises" + (if (workout.rounds > 1) ", ${workout.rounds} rounds. " else ". ") +
                        when {
                            intensity.tier < 0 -> "Easier variations."
                            intensity.tier > 0 -> "Harder variations."
                            else -> ""
                        },
                    style = Type.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("One-minute warm-up first", style = Type.body, modifier = Modifier.weight(1f))
                    Switch(
                        checked = warmUp,
                        onCheckedChange = onWarmUp,
                        colors = SwitchDefaults.colors(checkedTrackColor = Signal, checkedThumbColor = Ink),
                    )
                }
                Rule()
            }
            items(workout.exercises) { base -> ExerciseRow(base, base.forTier(intensity.tier)) }
        }
        SignalButton("Start workout", Modifier.padding(horizontal = ScreenPad, vertical = 12.dp)) { onStart(intensity) }
    }
}

@Composable
private fun ExerciseRow(base: Exercise, ex: Exercise) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FigureStage(ex, Modifier.size(width = 92.dp, height = 56.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = Type.row)
                Text(ex.muscles, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ex !== base) Text("Takes the place of ${base.name}", style = Type.small, color = accentText())
            }
            Text(if (open) "⌃" else "⌄", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (open) {
            Text(ex.how, style = Type.body, modifier = Modifier.padding(top = 12.dp))
            Text(ex.tip, style = Type.body, color = accentText(), modifier = Modifier.padding(top = 6.dp))
        }
    }
    Rule()
}

@Composable
fun DoneScreen(workout: Workout, seconds: Int, streak: Int, note: String?, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(ScreenPad),
        verticalArrangement = Arrangement.Center,
    ) {
        val pop = remember { Animatable(0.6f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        Box(
            Modifier.size(84.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape).background(Signal),
            contentAlignment = Alignment.Center,
        ) { Text("✓", fontSize = 46.sp, color = Ink, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(24.dp))
        Text("Done.", style = Type.hero)
        Text(workout.name, style = Type.body, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))
        Rule()
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${(seconds + 30) / 60}", style = Type.number)
                Text("minutes", style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text("$streak", style = Type.number)
                Text("day streak", style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Rule()
        if (note != null) Text(note, style = Type.row, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Rest these muscles for a day. Next time, slow your reps down.",
            style = Type.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
        )
        SignalButton("Back to start", onClick = onDone)
    }
}
