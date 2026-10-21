package com.fit15.app

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class Tab(val label: String) { TODAY("Today"), WORKOUTS("Workouts"), PROGRESS("Progress") }

/** Het beginscherm: drie tabbladen met een balk onderin. */
@Composable
fun HomeRoot(
    tab: Tab,
    onTab: (Tab) -> Unit,
    store: Store,
    history: List<Store.Entry>,
    programDone: Int,
    update: UpdateInfo?,
    onOpen: (Workout) -> Unit,
    onProgram: () -> Unit,
    onRestartProgram: () -> Unit,
    onMuscles: () -> Unit,
    onSettings: () -> Unit,
    onLibrary: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.TODAY -> TodayTab(store, history, programDone, update, onOpen, onProgram, onRestartProgram, onSettings)
                Tab.WORKOUTS -> WorkoutsTab(onOpen, onLibrary)
                Tab.PROGRESS -> ProgressTab(history, programDone, onMuscles)
            }
        }
        BottomBar(tab, onTab)
    }
}

@Composable
private fun BottomBar(tab: Tab, onTab: (Tab) -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        Rule()
        Row(Modifier.fillMaxWidth()) {
            for (t in Tab.entries) {
                val selected = t == tab
                Column(
                    Modifier.weight(1f).clickable { onTab(t) }.padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(36.dp).height(3.dp).background(if (selected) Signal else Color.Transparent))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        t.label,
                        style = Type.row,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Vandaag

@Composable
private fun TodayTab(
    store: Store,
    history: List<Store.Entry>,
    programDone: Int,
    update: UpdateInfo?,
    onOpen: (Workout) -> Unit,
    onProgram: () -> Unit,
    onRestartProgram: () -> Unit,
    onSettings: () -> Unit,
) {
    val weekDone = thisWeekCount(history)
    val goal = store.weeklyGoal
    val streak = streakOf(history)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = ScreenPad, end = ScreenPad, top = 8.dp, bottom = 32.dp)) {
        if (update != null) item { UpdateBanner(update) }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Fit15", style = Type.title, color = accentText(), modifier = Modifier.weight(1f))
                Text(
                    "Settings",
                    style = Type.row,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onSettings).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )
            }
        }
        item { Hero(programDone, onProgram, onRestartProgram) }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                GoalRing(weekDone, goal, Modifier.size(64.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (weekDone >= goal) "Weekly goal reached" else "$weekDone of $goal workouts this week",
                        style = Type.row,
                    )
                    Text(
                        if (streak > 0) "$streak day streak" else "Start a streak today",
                        style = Type.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { WeekStrip(history) }
        item { SectionTitle("Short on time") }
        items(Workouts.quick) { w -> WorkoutRow(w) { onOpen(w) } }
    }
}

@Composable
private fun GoalRing(done: Int, goal: Int, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val frac = (done.toFloat() / goal).coerceIn(0f, 1f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(Signal, -90f, 360f * frac, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("$done", style = Type.number)
    }
}

// ---------------------------------------------------------------- Workouts

@Composable
private fun WorkoutsTab(onOpen: (Workout) -> Unit, onLibrary: () -> Unit) {
    var filter by remember { mutableStateOf("All") }
    val list = Workouts.all.filter { filter == "All" || it.category == filter }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = ScreenPad, end = ScreenPad, top = 8.dp, bottom = 32.dp)) {
        item { Text("Workouts", style = Type.title, modifier = Modifier.padding(top = 16.dp, bottom = 14.dp)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                items(Categories.filters) { f ->
                    val selected = f == filter
                    Text(
                        f,
                        style = Type.row,
                        color = if (selected) Ink else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Signal else Color.Transparent)
                            .border(1.dp, if (selected) Signal else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { filter = f }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Rule()
        }
        items(list) { w -> WorkoutRow(w) { onOpen(w) } }
        item {
            Row(Modifier.fillMaxWidth().clickable(onClick = onLibrary).padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("All exercises", style = Type.row)
                    Text("Look up how to do any move", style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Rule()
        }
    }
}

// ---------------------------------------------------------------- Voortgang

@Composable
private fun ProgressTab(history: List<Store.Entry>, programDone: Int, onMuscles: () -> Unit) {
    val best = bestStreak(history)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = ScreenPad, end = ScreenPad, top = 8.dp, bottom = 32.dp)) {
        item { Text("Progress", style = Type.title, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) }
        item {
            Rule()
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                BigStat("${history.size}", "workouts", Modifier.weight(1f))
                BigStat("${history.sumOf { it.minutes }}", "minutes", Modifier.weight(1f))
                BigStat("$best", "best streak", Modifier.weight(1f))
            }
            Rule()
        }
        item { MonthCalendar(history) }
        item { Rule(); MusclesRow(history, onMuscles); Rule() }
        item { SectionTitle("Milestones") }
        items(milestones(history, best, programDone)) { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape)
                        .background(if (m.done) Signal else Color.Transparent)
                        .border(1.dp, if (m.done) Signal else MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (m.done) Text("✓", style = Type.small, fontWeight = FontWeight.Bold, color = Ink) }
                Spacer(Modifier.width(14.dp))
                Text(m.title, style = Type.body, modifier = Modifier.weight(1f))
                if (!m.done) Text(m.progress, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Rule()
        }
        item { SectionTitle("History") }
        if (history.isEmpty()) {
            item {
                Text(
                    "Nothing here yet. Finish a workout and it shows up in this list.",
                    style = Type.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
        }
        items(history.reversed().take(12)) { e ->
            val date = LocalDate.ofEpochDay(e.day).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(date, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
                Text(Workouts.byId(e.workoutId)?.name ?: "Workout", style = Type.body, modifier = Modifier.weight(1f))
                Text("${e.minutes} min", style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Rule()
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = Type.number)
        Text(label, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private class Milestone(val title: String, val done: Boolean, val progress: String)

private fun milestones(history: List<Store.Entry>, best: Int, programDone: Int): List<Milestone> {
    val n = history.size
    fun count(title: String, have: Int, need: Int) = Milestone(title, have >= need, "${have.coerceAtMost(need)}/$need")
    return listOf(
        count("First workout", n, 1),
        count("3-day streak", best, 3),
        count("10 workouts", n, 10),
        count("7-day streak", best, 7),
        count("25 workouts", n, 25),
        count("Finish the 4-week program", programDone, Program.total),
    )
}

@Composable
private fun MonthCalendar(history: List<Store.Entry>) {
    var offset by remember { mutableIntStateOf(0) }
    val ym = YearMonth.now().plusMonths(offset.toLong())
    val days = history.map { it.day }.toSet()
    val today = LocalDate.now()
    val blanks = ym.atDay(1).dayOfWeek.value - 1
    val rows = (blanks + ym.lengthOfMonth() + 6) / 7
    Column(Modifier.padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 28.sp, modifier = Modifier.clickable { offset-- }.padding(end = 20.dp, top = 4.dp, bottom = 4.dp))
            Text(
                ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.year,
                style = Type.section,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                "›",
                fontSize = 28.sp,
                color = if (offset < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.clickable(enabled = offset < 0) { offset++ }.padding(start = 20.dp, top = 4.dp, bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            for (d in listOf("M", "T", "W", "T", "F", "S", "S")) {
                Text(d, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                for (c in 0..6) {
                    val day = r * 7 + c - blanks + 1
                    Box(Modifier.weight(1f).height(38.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..ym.lengthOfMonth()) {
                            val date = ym.atDay(day)
                            val trained = date.toEpochDay() in days
                            val isToday = date == today
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .background(if (trained) Signal else Color.Transparent)
                                    .then(if (isToday) Modifier.border(2.dp, if (trained) Signal else MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = Type.small,
                                    fontWeight = if (trained || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (trained) Ink else if (date.isAfter(today)) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
