package com.fit15.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Korte samenvatting voor op het beginscherm. */
fun muscleSummary(sets: Map<Muscle, Float>): String {
    if (sets.isEmpty()) return "No workouts in the last 7 days yet. Finish one and your muscles light up here."
    val top = Muscle.entries.filter { (sets[it] ?: 0f) > 0f }.sortedByDescending { sets[it] }.take(3)
    val missing = Muscle.entries.filter { (sets[it] ?: 0f) <= 0f }
    val a = "Most worked: " + top.joinToString(", ") { it.label }
    return if (missing.isEmpty()) a else "$a\nNot trained yet: " + missing.take(3).joinToString(", ") { it.label }
}

@Composable
fun MusclesScreen(history: List<Store.Entry>, onBack: () -> Unit) {
    val sets = weeklySets(history)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Back", fontSize = 16.sp) }
            Text("Your muscles", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Last 7 days. The more sets, the brighter the colour.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
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
                    BodyMap(sets, height = 320.dp, modifier = Modifier.fillMaxWidth())
                    HeatLegend(Modifier.padding(top = 16.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            Text("Sets per muscle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        for (m in Muscle.entries) {
            item {
                val v = sets[m] ?: 0f
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text(m.label, modifier = Modifier.weight(1f))
                        Text(
                            if (v <= 0f) "not trained" else "${(v * 10).toInt() / 10f}".removeSuffix(".0") + " sets",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (v / FULL_SETS).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
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
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
    }
}
