package com.fit15.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.row)
            Text(subtitle, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Signal, checkedThumbColor = Ink),
        )
    }
    Rule()
}

@Composable
fun SettingsScreen(store: Store, onBack: () -> Unit, onRedoSetup: () -> Unit) {
    val ctx = LocalContext.current
    var sound by remember { mutableStateOf(store.soundOn) }
    var voice by remember { mutableStateOf(store.voiceOn) }
    var vibrate by remember { mutableStateOf(store.vibrateOn) }
    var goal by remember { mutableIntStateOf(store.weeklyGoal) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = ScreenPad, vertical = 8.dp)) {
        item {
            BackLink(onBack)
            Text("Settings", style = Type.title, modifier = Modifier.padding(bottom = 20.dp))
            ReminderRow(store)
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Weekly goal", style = Type.row)
                    Text("Workouts per week", style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Stepper("−") { goal = (goal - 1).coerceAtLeast(1); store.weeklyGoal = goal }
                Text("$goal", style = Type.number, modifier = Modifier.padding(horizontal = 16.dp))
                Stepper("+") { goal = (goal + 1).coerceAtMost(7); store.weeklyGoal = goal }
            }
            Rule()
        }
        item {
            SwitchRow("Beeps", "A tone at the last three seconds and at every change", sound) { sound = it; store.soundOn = it }
            SwitchRow("Voice", "Says the name of the next exercise", voice) { voice = it; store.voiceOn = it }
            SwitchRow("Vibration", "A buzz when the exercise changes", vibrate) { vibrate = it; store.vibrateOn = it }
            Column(Modifier.fillMaxWidth().clickable(onClick = onRedoSetup).padding(vertical = 12.dp)) {
                Text("Redo the start questions", style = Type.row)
                Text(
                    "Change your goal, level and days. This restarts your 4-week program.",
                    style = Type.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Rule()
            Text(
                "Fit15 ${currentVersion(ctx)}. Free, no account, no ads. Updates come from GitHub.",
                style = Type.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = Type.section,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

@Composable
fun LibraryScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = ScreenPad, vertical = 8.dp)) {
        item {
            BackLink(onBack)
            Text("All exercises", style = Type.title)
            Text(
                "${Exercises.library.size} moves. Tap one to see how it is done.",
                style = Type.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Rule()
        }
        items(Exercises.library) { ex -> ExerciseRow(ex, ex) }
    }
}
