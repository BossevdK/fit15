package com.fit15.app

import androidx.compose.foundation.Canvas
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

/** Zoveel sets per week geeft een spiergroep de volle kleur. */
private const val FULL_SETS = 12f

private const val BW = 100f
private const val BH = 208f

/** Tekent een voor- of achterkant van het lichaam; getrainde spiergroepen worden oranje (hoe meer, hoe feller). */
private fun DrawScope.drawBody(front: Boolean, sets: Map<Muscle, Float>, hot: Color, base: Color) {
    val s = min(size.width / BW, size.height / BH)
    val ox = (size.width - BW * s) / 2f
    val oy = (size.height - BH * s) / 2f
    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, c: Color) =
        drawLine(c, p(x1, y1), p(x2, y2), strokeWidth = w * s, cap = StrokeCap.Round)
    fun dot(x: Float, y: Float, r: Float, c: Color) = drawCircle(c, r * s, p(x, y))
    fun col(m: Muscle): Color? {
        val v = sets[m] ?: 0f
        if (v <= 0f) return null
        return hot.copy(alpha = 0.3f + 0.7f * (v / FULL_SETS).coerceIn(0f, 1f))
    }

    // silhouet
    dot(50f, 15f, 10f, base)
    line(50f, 24f, 50f, 34f, 8f, base)
    val torso = Path().apply {
        moveTo(p(30f, 36f).x, p(30f, 36f).y)
        quadraticBezierTo(p(50f, 31f).x, p(50f, 31f).y, p(70f, 36f).x, p(70f, 36f).y)
        lineTo(p(73f, 54f).x, p(73f, 54f).y)
        lineTo(p(67f, 98f).x, p(67f, 98f).y)
        lineTo(p(63f, 114f).x, p(63f, 114f).y)
        lineTo(p(37f, 114f).x, p(37f, 114f).y)
        lineTo(p(33f, 98f).x, p(33f, 98f).y)
        lineTo(p(27f, 54f).x, p(27f, 54f).y)
        close()
    }
    drawPath(torso, base)
    for (side in listOf(1f, -1f)) {
        fun x(v: Float) = 50f + (v - 50f) * side
        line(x(24f), 40f, x(15f), 82f, 12f, base)
        line(x(15f), 82f, x(11f), 120f, 9f, base)
        line(x(42f), 112f, x(40f), 158f, 18f, base)
        line(x(40f), 158f, x(40f), 202f, 12f, base)
    }

    // spiergroepen
    for (side in listOf(1f, -1f)) {
        fun x(v: Float) = 50f + (v - 50f) * side
        col(Muscle.SHOULDERS)?.let { dot(x(25f), 43f, 8.5f, it) }
        if (front) {
            col(Muscle.CHEST)?.let { c ->
                val pec = Path().apply {
                    moveTo(p(x(30f), 42f).x, p(x(30f), 42f).y)
                    quadraticBezierTo(p(x(40f), 40f).x, p(x(40f), 40f).y, p(x(49f), 44f).x, p(x(49f), 44f).y)
                    lineTo(p(x(49f), 62f).x, p(x(49f), 62f).y)
                    quadraticBezierTo(p(x(38f), 66f).x, p(x(38f), 66f).y, p(x(30f), 58f).x, p(x(30f), 58f).y)
                    close()
                }
                drawPath(pec, c)
            }
            col(Muscle.CORE)?.let { c ->
                line(x(36f), 70f, x(37.5f), 98f, 5f, c)
            }
            col(Muscle.QUADS)?.let { line(x(41f), 120f, x(40f), 153f, 15f, it) }
            col(Muscle.CALVES)?.let { line(x(40f), 165f, x(40f), 196f, 8f, it) }
        } else {
            col(Muscle.TRICEPS)?.let { line(x(23f), 48f, x(16f), 80f, 9f, it) }
            col(Muscle.UPPER_BACK)?.let { c ->
                val half = Path().apply {
                    moveTo(p(50f, 36f).x, p(50f, 36f).y)
                    lineTo(p(x(35f), 40f).x, p(x(35f), 40f).y)
                    lineTo(p(x(29f), 58f).x, p(x(29f), 58f).y)
                    lineTo(p(x(40f), 80f).x, p(x(40f), 80f).y)
                    lineTo(p(50f, 76f).x, p(50f, 76f).y)
                    close()
                }
                drawPath(half, c)
            }
            col(Muscle.GLUTES)?.let { dot(x(43f), 121f, 10f, it) }
            col(Muscle.HAMSTRINGS)?.let { line(x(42f), 136f, x(41f), 154f, 14f, it) }
            col(Muscle.CALVES)?.let { line(x(40f), 165f, x(40f), 194f, 11f, it) }
        }
    }
    if (front) {
        col(Muscle.CORE)?.let {
            drawRoundRect(it, p(43f, 66f), Size(14f * s, 38f * s), CornerRadius(5f * s))
        }
    } else {
        col(Muscle.LOWER_BACK)?.let {
            drawRoundRect(it, p(42f, 82f), Size(16f * s, 22f * s), CornerRadius(5f * s))
        }
    }
}

@Composable
fun BodyMap(sets: Map<Muscle, Float>, height: Dp, modifier: Modifier = Modifier) {
    val hot = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        for (front in listOf(true, false)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.height(height).width(height * (BW / BH))) { drawBody(front, sets, hot, base) }
                Text(
                    if (front) "Front" else "Back",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

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
        item { BodyMap(sets, height = 340.dp, modifier = Modifier.fillMaxWidth()) }
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
                        color = MaterialTheme.colorScheme.primary,
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
