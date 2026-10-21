package com.fit15.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Korte samenvatting voor op het beginscherm. */
fun muscleSummary(sets: Map<Muscle, Float>): String {
    if (sets.isEmpty()) return "No workouts in the last 7 days yet. Finish one and your muscles light up here."
    val top = Muscle.entries.filter { (sets[it] ?: 0f) > 0f }.sortedByDescending { sets[it] }.take(3)
    val missing = Muscle.entries.filter { (sets[it] ?: 0f) <= 0f }
    val a = "Most worked: " + top.joinToString(", ") { it.label }
    return if (missing.isEmpty()) a else "$a\nNot trained yet: " + missing.take(3).joinToString(", ") { it.label }
}

private fun setsText(v: Float) =
    if (v <= 0f) "not trained" else "${(v * 10).toInt() / 10f}".removeSuffix(".0") + " sets"

@Composable
fun MusclesScreen(history: List<Store.Entry>, onBack: () -> Unit, onOpenWorkout: (Workout) -> Unit) {
    val sets = weeklySets(history)
    var selected by remember { mutableStateOf<Muscle?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = ScreenPad, vertical = 8.dp)) {
        item {
            BackLink(onBack)
            Text("Your muscles", style = Type.title)
            Text(
                "Last 7 days. Tap a muscle to see what trains it.",
                style = Type.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    BodyMap(
                        sets, height = 320.dp, modifier = Modifier.fillMaxWidth(),
                        selected = selected,
                        onTap = { tapped -> selected = if (tapped == selected) null else tapped },
                    )
                    HeatLegend(Modifier.padding(top = 16.dp))
                }
            }
        }
        val sel = selected
        if (sel != null) {
            item { MuscleSuggestions(sel, sets[sel] ?: 0f, onOpenWorkout) }
        }
        item {
            Text("Sets per muscle", style = Type.section, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
        }
        for (m in Muscle.entries) {
            item {
                val v = sets[m] ?: 0f
                Column(Modifier.fillMaxWidth().clickable { selected = if (selected == m) null else m }.padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            m.label,
                            style = Type.body,
                            fontWeight = if (m == selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        Text(setsText(v), style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { (v / FULL_SETS).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = heatColor(v) ?: MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
        item {
            Text(
                "Aim for about 10 to 12 sets per muscle per week to keep growing. " +
                    "Your upper back is hard to train without equipment: Superman and Bird dog help, " +
                    "and a backpack row with a heavy bag is a good extra.",
                style = Type.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

/** Wat je kunt doen voor de spier waarop je tikte: oefeningen en de workouts die hem het meest trainen. */
@Composable
private fun MuscleSuggestions(muscle: Muscle, sets: Float, onOpenWorkout: (Workout) -> Unit) {
    val workouts = Workouts.all
        .map { w -> w to w.exercises.count { muscle in it.primary } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(4)
    val moves = Exercises.library.filter { muscle in it.primary }.take(6)
    Column(Modifier.padding(top = 20.dp)) {
        Rule()
        Text("Train your ${muscle.label.lowercase()}", style = Type.section, modifier = Modifier.padding(top = 14.dp))
        Text(
            "This week: ${setsText(sets)}.",
            style = Type.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (moves.isNotEmpty()) {
            Text(
                "Moves: " + moves.joinToString(", ") { it.name },
                style = Type.body,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("Workouts that hit it hardest", style = Type.row, modifier = Modifier.padding(top = 8.dp))
        Rule()
        for ((w, _) in workouts) WorkoutRow(w) { onOpenWorkout(w) }
    }
}
