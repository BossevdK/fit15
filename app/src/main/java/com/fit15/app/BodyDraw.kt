package com.fit15.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Zoveel sets per week geeft een spiergroep de volle kleur. */
const val FULL_SETS = 12f
private const val BW = 100f
private const val BH = 208f

private val Cool = Color(0xFF4DA3E0) // weinig
private val Warm = Color(0xFFF2B33D) // gemiddeld
private val Hot = Color(0xFFE5392B) // veel

/** null = niet getraind. Koel (blauw), warm (amber), heet (rood). */
fun heatColor(sets: Float): Color? {
    if (sets <= 0f) return null
    val t = (sets / FULL_SETS).coerceIn(0f, 1f)
    return if (t < 0.5f) lerp(Cool, Warm, t * 2f) else lerp(Warm, Hot, (t - 0.5f) * 2f)
}

private fun pth(d: String) = PathParser().parsePathString(d).toPath()

private class Part(val m: Muscle, d: String) {
    val path = pth(d)
    val b = path.getBounds()
}

// Rechterkant van het lichaam (x >= 50); de linkerkant wordt gespiegeld getekend.
private val SIL = pth(
    "M50 22 L56 22 C56.5 28 57 31 60 33 C68 34 76 35 80 40 C84 46 86 60 87 74 " +
        "C88 88 91 100 92 114 C93 120 88 124 86 118 C85 106 82 90 80 78 C78 68 75 62 72 56 " +
        "C71 70 68 80 66 90 C66 100 70 108 70 118 C71.5 130 70 144 67 157 C68 172 67 184 65 196 " +
        "C66 202 66 205 62 205.5 L55 205.5 C52.5 205.5 52 203 53 200 C54 197 54 195 54 192 " +
        "C53 182 53 172 54 160 C53 148 52 134 50 124 Z",
)
private val HAIR = pth("M41.8 12 C41.2 5 45 2.4 50 2.4 C55 2.4 58.8 5 58.2 12 C56 8.5 44 8.5 41.8 12 Z")

private const val DELT = "M64 34 C71 33.5 77 35 80.5 40 C83.5 45 84.5 52 84 58 C80 58 76.5 55 74 50 C72 44 68 38 64 34 Z"

private val FRONT = listOf(
    Part(Muscle.CHEST, "M51 38 C58 36.5 64 37 69 41 C73 45 74 50 72.5 56 C66 61 58 62 51 59 Z"),
    Part(Muscle.CORE, "M51.5 64 C55 63.5 58.5 64.5 60.5 66.5 C61 76 61 86 60 95 C57.5 100 54 103 51.5 104 Z"),
    Part(Muscle.CORE, "M62 68 C65 70 68 74 68 80 C67.5 86 66.5 92 65 97 C62.5 93 61.8 80 62 68 Z"),
    Part(Muscle.QUADS, "M55 121 C60 118.5 66 119 70 122 C71 132 69.5 144 67 153 C64 157 59.5 157 56.5 154 C54.5 144 53 134 55 121 Z"),
    Part(Muscle.CALVES, "M57 166 C61 164 65 165 66.5 168 C66.5 178 65.5 187 64.5 192 C61 193 58 193 56 192 C55 184 55.5 174 57 166 Z"),
    Part(Muscle.SHOULDERS, DELT),
)
private val BACK = listOf(
    Part(Muscle.UPPER_BACK, "M51 29 C56 30 60 32 64 35 C69 37 71 43 72 50 C73 58 71 66 68 73 C64 76 58 76.5 51 75 Z"),
    Part(Muscle.LOWER_BACK, "M51 77 C56 77.5 61 76.5 64.5 74 C65 80 65 86 64.3 91 C62 97 57 99 51 99 Z"),
    Part(Muscle.GLUTES, "M51 100 C57 98 63.5 99.5 67.5 105 C69.5 111 69 119 65.5 124 C60 127 54 125.5 51 122 Z"),
    Part(Muscle.HAMSTRINGS, "M52.5 126 C58 127 64 126 69.5 124.5 C70.5 134 69 146 66.5 156 C63 159 59 159 56 157 C54.5 147 53 138 52.5 126 Z"),
    Part(Muscle.CALVES, "M56.5 163 C60 161 64.5 162 66.5 166 C67 174 65.5 184 64 191 C61 193 58.5 193 56.5 191 C54.5 182 54.5 172 56.5 163 Z"),
    Part(Muscle.SHOULDERS, DELT),
    Part(Muscle.TRICEPS, "M75 56 C79 59 83 59 85.2 59 C86.2 66 87 72 87.2 77 C85 80 82 80 80.3 78 C79 72 77 64 75 56 Z"),
)

/** Welke spier zit er op dit punt (in pixels van de tekening)? Kiest de kleinste vorm waar het punt in valt. */
fun hitMuscle(front: Boolean, x: Float, y: Float, w: Float, h: Float): Muscle? {
    val s = min(w / BW, h / BH)
    var px = (x - (w - BW * s) / 2f) / s
    val py = (y - (h - BH * s) / 2f) / s
    if (px < 50f) px = 100f - px // de linkerkant is een spiegeling van de rechterkant
    val at = Offset(px, py)
    return (if (front) FRONT else BACK)
        .filter { it.b.contains(at) }
        .minByOrNull { it.b.width * it.b.height }
        ?.m
}

private fun DrawScope.drawBody(front: Boolean, sets: Map<Muscle, Float>, neutral: Color, ink: Color, seam: Color, selected: Muscle?) {
    val s = min(size.width / BW, size.height / BH)
    withTransform({
        translate((size.width - BW * s) / 2f, (size.height - BH * s) / 2f)
        scale(s, s, Offset.Zero)
    }) {
        drawOval(neutral, Offset(41.7f, 2.3f), Size(16.6f, 20.4f))
        drawPath(HAIR, ink.copy(alpha = if (front) 0.16f else 0.32f))
        for (mirror in listOf(false, true)) {
            withTransform({ if (mirror) scale(-1f, 1f, Offset(50f, 0f)) }) {
                drawPath(SIL, neutral)
                withTransform({ clipPath(SIL) }) {
                    for (p in if (front) FRONT else BACK) {
                        val c = heatColor(sets[p.m] ?: 0f)
                        if (c == null) {
                            drawPath(p.path, ink.copy(alpha = 0.05f))
                            drawPath(p.path, ink.copy(alpha = 0.12f), style = Stroke(0.45f))
                        } else {
                            drawPath(
                                p.path,
                                Brush.verticalGradient(
                                    listOf(lerp(c, Color.White, 0.22f), lerp(c, Color.Black, 0.12f)),
                                    startY = p.b.top, endY = p.b.bottom,
                                ),
                            )
                            drawPath(p.path, seam, style = Stroke(0.5f))
                        }
                    }
                    if (selected != null) {
                        for (p in if (front) FRONT else BACK) {
                            if (p.m == selected) drawPath(p.path, Signal, style = Stroke(1.4f, join = StrokeJoin.Round))
                        }
                    }
                    val fine = ink.copy(alpha = 0.16f)
                    if (front) {
                        for (y in listOf(73f, 82f, 91f)) drawLine(fine, Offset(51.5f, y), Offset(60.5f, y), 0.5f)
                        drawCircle(fine, 0.9f, Offset(50f, 92f))
                    } else {
                        drawLine(fine, Offset(50f, 26f), Offset(50f, 100f), 0.5f)
                    }
                }
                drawPath(SIL, ink.copy(alpha = 0.25f), style = Stroke(0.7f, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
fun BodyMap(
    sets: Map<Muscle, Float>,
    height: Dp,
    modifier: Modifier = Modifier,
    selected: Muscle? = null,
    onTap: ((Muscle?) -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    val neutral = lerp(c.surface, c.onSurface, 0.14f) // dichtbij maar niet doorzichtig
    val summary = muscleSummary(sets)
    Row(
        modifier.semantics { contentDescription = summary },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (front in listOf(true, false)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val tapModifier = if (onTap != null) {
                    Modifier.pointerInput(front) {
                        detectTapGestures { at ->
                            onTap(hitMuscle(front, at.x, at.y, size.width.toFloat(), size.height.toFloat()))
                        }
                    }
                } else {
                    Modifier
                }
                Canvas(Modifier.height(height).width(height * (BW / BH)).then(tapModifier)) {
                    drawBody(front, sets, neutral, c.onSurface, c.surface.copy(alpha = 0.7f), selected)
                }
                Text(if (front) "Front" else "Back", fontSize = 12.sp, color = c.onSurfaceVariant)
            }
        }
    }
}

/** Legenda: kleurbalk van koel naar warm plus "niet getraind". */
@Composable
fun HeatLegend(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(Cool, Warm, Hot))),
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1 set", fontSize = 12.sp, color = c.onSurfaceVariant)
            Text("6", fontSize = 12.sp, color = c.onSurfaceVariant)
            Text("12+ sets", fontSize = 12.sp, color = c.onSurfaceVariant)
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(lerp(c.surface, c.onSurface, 0.14f)))
            Spacer(Modifier.width(6.dp))
            Text("Not trained this week", fontSize = 12.sp, color = c.onSurfaceVariant)
        }
    }
}
