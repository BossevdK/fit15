package com.fit15.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

@Composable
private fun Figure(ex: Exercise, modifier: Modifier, playing: Boolean = true) {
    FigureStage(ex, modifier, playing)
}

@Composable
private fun cardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

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
    val minutes = history.sumOf { it.minutes }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (update != null) item { UpdateBanner(update) }
        item {
            Text("Fit15", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(
                "15-minute bodyweight workouts. No equipment, no cost.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Day streak", streak.toString(), Modifier.weight(1f))
                Stat("Workouts", history.size.toString(), Modifier.weight(1f))
                Stat("Minutes", minutes.toString(), Modifier.weight(1f))
            }
        }
        item { WeekStrip(history) }
        item { ProgramCard(programDone, onProgram, onRestartProgram) }
        item { MusclesCard(history, onMuscles) }
        item {
            Text("15-minute workouts", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }
        items(Workouts.main) { w -> WorkoutCard(w, onClick = { onOpen(w) }) }
        item {
            Text("Short on time? 7 minutes", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }
        items(Workouts.quick) { w -> WorkoutCard(w, onClick = { onOpen(w) }) }
        item { ReminderCard(store) }
        item {
            Text(
                "Tip: for muscle growth, train hard but rest the same muscles for a day before hitting them again. " +
                    "Slow, controlled reps beat fast sloppy ones.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun UpdateBanner(info: UpdateInfo) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Update available: v${info.version}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (info.notes.isNotBlank()) {
                Text(
                    info.notes,
                    fontSize = 13.sp,
                    maxLines = 4,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            msg?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(top = 6.dp))
            }
            Button(
                enabled = progress == null,
                onClick = {
                    scope.launch {
                        progress = 0f
                        msg = null
                        try {
                            val file = withContext(Dispatchers.IO) { downloadUpdate(ctx, info) { progress = it } }
                            progress = null
                            if (!installApk(ctx, file)) msg = "Allow installing from Fit15 in the settings that just opened, then tap Install again."
                        } catch (e: Exception) {
                            progress = null
                            msg = "Update failed: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.padding(top = 10.dp),
            ) {
                val p = progress
                Text(if (p != null) "Downloading ${(p * 100).toInt()}%" else "Download and install")
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeekStrip(history: List<Store.Entry>) {
    val days = history.map { it.day }.toSet()
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 6 downTo 0) {
            val d = today.minusDays(i.toLong())
            val done = d.toEpochDay() in days
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .then(if (i == 0) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (done) "✓" else d.dayOfMonth.toString(),
                        fontSize = if (done) 16.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramCard(done: Int, onStart: () -> Unit, onRestart: () -> Unit) {
    val next = Program.next(done)
    val c = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val end = if (dark) Color(0xFFE5703F) else lerp(c.primary, Color.Black, 0.25f)
    val fg = c.onPrimary
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.background(Brush.linearGradient(listOf(c.primary, end))).padding(20.dp)) {
            Text(Program.NAME.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = fg.copy(alpha = 0.8f))
            if (next != null) {
                Text(
                    "Week ${next.weekIndex + 1} of ${Program.weeks.size}: ${next.week.title}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = fg,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "Session ${next.sessionIndex + 1} of ${next.week.sessions.size}: ${next.workout.name} · ${next.week.intensity.hint}",
                    fontSize = 14.sp,
                    color = fg,
                    modifier = Modifier.padding(top = 2.dp),
                )
                LinearProgressIndicator(
                    progress = { done.toFloat() / Program.total },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = fg,
                    trackColor = fg.copy(alpha = 0.3f),
                )
                Text(
                    "$done of ${Program.total} sessions done. Train 3 times a week with a rest day in between.",
                    fontSize = 12.sp,
                    color = fg.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 6.dp),
                )
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = fg, contentColor = c.primary),
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text("Start today's session", fontWeight = FontWeight.Bold) }
            } else {
                Text("Program complete!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = fg, modifier = Modifier.padding(top = 2.dp))
                Text(
                    "You finished all ${Program.total} sessions. Start again and aim for cleaner, slower reps.",
                    fontSize = 14.sp,
                    color = fg,
                )
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = fg, contentColor = c.primary),
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text("Start again", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun MusclesCard(history: List<Store.Entry>, onClick: () -> Unit) {
    val sets = weeklySets(history)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BodyMap(sets, height = 120.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Your week", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    muscleSummary(sets),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text("Tap for the full map", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun WorkoutCard(w: Workout, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Figure(w.exercises[0], Modifier.size(width = 100.dp, height = 60.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(w.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(w.focus, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${w.minutes} min · ${w.exercises.size} exercises" + if (w.rounds > 1) " · ${w.rounds} rounds" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderCard(store: Store) {
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

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "At %02d:%02d (tap the time to change it)".format(hour, minute),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
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
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
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
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text("‹ Back", fontSize = 16.sp) }
                Text(workout.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(workout.focus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                if (program != null) {
                    Text("${Program.NAME} · Week ${program.weekIndex + 1}: ${program.week.title}", fontWeight = FontWeight.Bold)
                } else {
                    Text("Intensity", fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (l in Level.entries) {
                            FilterChip(selected = l == level, onClick = { onLevel(l) }, label = { Text(l.label) })
                        }
                    }
                }
                Text(
                    "${intensity.hint} · ${workout.exercises.size} exercises" +
                        (if (workout.rounds > 1) " × ${workout.rounds} rounds" else "") +
                        when {
                            intensity.tier < 0 -> " · easier variations"
                            intensity.tier > 0 -> " · harder variations"
                            else -> ""
                        },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Add a 1-minute warm-up", modifier = Modifier.weight(1f))
                    Switch(checked = warmUp, onCheckedChange = onWarmUp)
                }
            }
            items(workout.exercises) { base -> ExerciseRow(base, base.forTier(intensity.tier)) }
        }
        Button(
            onClick = { onStart(intensity) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Start workout", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ExerciseRow(base: Exercise, ex: Exercise) {
    var open by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable { open = !open },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Figure(ex, Modifier.size(width = 100.dp, height = 60.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ex.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(ex.muscles, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (ex !== base) {
                        Text(
                            "Variation of ${base.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(if (open) "▲" else "▼", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (open) {
                Spacer(Modifier.height(10.dp))
                Text(ex.how, fontSize = 14.sp)
                Text(
                    ex.tip,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun DoneScreen(workout: Workout, seconds: Int, streak: Int, note: String?, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val pop = remember { Animatable(0.6f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Box(Modifier.size(130.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)))
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.2f))),
                ),
                contentAlignment = Alignment.Center,
            ) { Text("✓", fontSize = 54.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(24.dp))
        Text("Workout complete!", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(workout.name, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Minutes", ((seconds + 30) / 60).toString(), Modifier.width(110.dp))
            Stat("Day streak", streak.toString(), Modifier.width(110.dp))
        }
        if (note != null) {
            Text(
                note,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        Text(
            "Rest the muscles you just trained for a day, and slow your reps down to keep them growing.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}
