package com.fit15.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Een houding van het stokfiguurtje, kijkend naar rechts. Alle hoeken zijn in graden, tegen de klok in vanaf
 * "naar rechts" (90 = omhoog, -90 = omlaag). Het figuur wordt altijd op de grond gezet (laagste punt = vloer).
 * Ledematen A zijn de voorste (dikke), B de achterste (lichtere).
 */
data class Pose(
    val hx: Float,               // x van de heup
    val torso: Float,            // heup -> nek
    val uaA: Float = -90f, val faA: Float = -90f,   // bovenarm / onderarm
    val uaB: Float = -90f, val faB: Float = -90f,
    val thA: Float = -90f, val shA: Float = -90f,   // bovenbeen / onderbeen
    val thB: Float = -90f, val shB: Float = -90f,
    val toeA: Float = 0f, val toeB: Float = 0f,     // voet: hoek onder de horizontaal (negatief = tenen omhoog)
    val headDir: Float? = null,                     // richting nek -> hoofd (standaard: dezelfde als de romp)
    val lift: Float = 0f,                           // extra hoogte boven de grond (sprong)
)

enum class Prop { NONE, WALL, CHAIR, CHAIR_FEET }

private const val TORSO = 14f
private const val UPPER_ARM = 8f
private const val FORE_ARM = 8f
private const val THIGH = 10f
private const val SHIN = 10f
private const val FOOT = 3.5f
private const val HEAD_R = 3.4f
private const val HEAD_DIST = 5.2f

/** Wereldruimte: 100 breed, 60 hoog, y omhoog, de vloer ligt op y = FLOOR. */
private const val WORLD_W = 100f
private const val WORLD_H = 60f
private const val FLOOR = 6f

private fun dir(deg: Float): Offset {
    val r = Math.toRadians(deg.toDouble())
    return Offset(cos(r).toFloat(), sin(r).toFloat())
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

fun lerpPose(a: Pose, b: Pose, t: Float) = Pose(
    hx = lerp(a.hx, b.hx, t), torso = lerp(a.torso, b.torso, t),
    uaA = lerp(a.uaA, b.uaA, t), faA = lerp(a.faA, b.faA, t),
    uaB = lerp(a.uaB, b.uaB, t), faB = lerp(a.faB, b.faB, t),
    thA = lerp(a.thA, b.thA, t), shA = lerp(a.shA, b.shA, t),
    thB = lerp(a.thB, b.thB, t), shB = lerp(a.shB, b.shB, t),
    toeA = lerp(a.toeA, b.toeA, t), toeB = lerp(a.toeB, b.toeB, t),
    headDir = lerp(a.headDir ?: a.torso, b.headDir ?: b.torso, t),
    lift = lerp(a.lift, b.lift, t),
)

private class Skeleton(
    val headC: Offset, val neck: Offset, val hip: Offset,
    val elbowA: Offset, val handA: Offset, val elbowB: Offset, val handB: Offset,
    val kneeA: Offset, val ankleA: Offset, val toeA: Offset,
    val kneeB: Offset, val ankleB: Offset, val toeB: Offset,
)

private fun solve(p: Pose): Skeleton {
    val hip = Offset(p.hx, 0f)
    val neck = hip + dir(p.torso) * TORSO
    val headC = neck + dir(p.headDir ?: p.torso) * HEAD_DIST
    val elbowA = neck + dir(p.uaA) * UPPER_ARM
    val handA = elbowA + dir(p.faA) * FORE_ARM
    val elbowB = neck + dir(p.uaB) * UPPER_ARM
    val handB = elbowB + dir(p.faB) * FORE_ARM
    val kneeA = hip + dir(p.thA) * THIGH
    val ankleA = kneeA + dir(p.shA) * SHIN
    val toeA = ankleA + dir(-p.toeA) * FOOT
    val kneeB = hip + dir(p.thB) * THIGH
    val ankleB = kneeB + dir(p.shB) * SHIN
    val toeB = ankleB + dir(-p.toeB) * FOOT
    // laagste punt (zonder hoofd) op de vloer zetten
    val low = minOf(
        hip.y, elbowA.y, handA.y, elbowB.y, handB.y, kneeA.y, ankleA.y, toeA.y, kneeB.y, ankleB.y, toeB.y,
    )
    val up = Offset(0f, FLOOR - low + p.lift)
    return Skeleton(
        headC + up, neck + up, hip + up,
        elbowA + up, handA + up, elbowB + up, handB + up,
        kneeA + up, ankleA + up, toeA + up, kneeB + up, ankleB + up, toeB + up,
    )
}

private fun DrawScope.drawFigure(
    pose: Pose, prop: Prop, front: Boolean, anchorX: Float, body: Color, ground: Color,
) {
    val scale = min(size.width / WORLD_W, size.height / WORLD_H)
    val ox = (size.width - WORLD_W * scale) / 2f
    val oy = (size.height - WORLD_H * scale) / 2f
    fun pt(o: Offset) = Offset(ox + o.x * scale, oy + (WORLD_H - o.y) * scale)
    val w = 2.6f * scale

    // vloer
    drawLine(ground, pt(Offset(4f, FLOOR - 1.3f)), pt(Offset(96f, FLOOR - 1.3f)), strokeWidth = 0.8f * scale, cap = StrokeCap.Round)

    // attributen (muur / stoel)
    when (prop) {
        Prop.WALL -> {
            val x = anchorX - 4.6f
            drawLine(ground, pt(Offset(x, FLOOR - 1.3f)), pt(Offset(x, 56f)), strokeWidth = 1.6f * scale, cap = StrokeCap.Round)
        }
        Prop.CHAIR -> {
            val l = anchorX - 11.5f
            val r = anchorX - 1.5f
            val top = FLOOR - 1.3f + 10f
            drawLine(ground, pt(Offset(l, top)), pt(Offset(r, top)), strokeWidth = 1.8f * scale, cap = StrokeCap.Round)
            drawLine(ground, pt(Offset(l + 0.5f, top)), pt(Offset(l + 0.5f, FLOOR - 1.3f)), strokeWidth = 1.2f * scale, cap = StrokeCap.Round)
            drawLine(ground, pt(Offset(r - 0.5f, top)), pt(Offset(r - 0.5f, FLOOR - 1.3f)), strokeWidth = 1.2f * scale, cap = StrokeCap.Round)
        }
        Prop.CHAIR_FEET -> {
            val l = anchorX - 31f
            val r = anchorX - 19f
            val top = FLOOR - 1.3f + 10f
            drawLine(ground, pt(Offset(l, top)), pt(Offset(r, top)), strokeWidth = 1.8f * scale, cap = StrokeCap.Round)
            drawLine(ground, pt(Offset(l + 0.5f, top)), pt(Offset(l + 0.5f, FLOOR - 1.3f)), strokeWidth = 1.2f * scale, cap = StrokeCap.Round)
            drawLine(ground, pt(Offset(r - 0.5f, top)), pt(Offset(r - 0.5f, FLOOR - 1.3f)), strokeWidth = 1.2f * scale, cap = StrokeCap.Round)
        }
        Prop.NONE -> Unit
    }

    val s = solve(pose)
    val far = if (front) body else body.copy(alpha = 0.42f)
    fun seg(a: Offset, b: Offset, c: Color) = drawLine(c, pt(a), pt(b), strokeWidth = w, cap = StrokeCap.Round)

    // achterste ledematen
    seg(s.neck, s.elbowB, far); seg(s.elbowB, s.handB, far)
    seg(s.hip, s.kneeB, far); seg(s.kneeB, s.ankleB, far); seg(s.ankleB, s.toeB, far)
    // romp en hoofd
    seg(s.hip, s.neck, body)
    drawCircle(body, radius = HEAD_R * scale, center = pt(s.headC), style = Stroke(width = w * 0.85f))
    // voorste ledematen
    seg(s.neck, s.elbowA, body); seg(s.elbowA, s.handA, body)
    seg(s.hip, s.kneeA, body); seg(s.kneeA, s.ankleA, body); seg(s.ankleA, s.toeA, body)
}

/** Speelt een oefening af: gaat zacht heen en weer tussen twee houdingen. */
@Composable
fun ExerciseFigure(
    exercise: Exercise,
    modifier: Modifier,
    body: Color,
    ground: Color,
    playing: Boolean = true,
) {
    val t by rememberInfiniteTransition(label = "fig").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(exercise.halfPeriodMs, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    // zachte in-en-uit beweging
    val e = if (playing) (0.5f - 0.5f * cos(Math.PI * t).toFloat()) else 0f
    Canvas(modifier) {
        drawFigure(lerpPose(exercise.a, exercise.b, e), exercise.prop, exercise.front, exercise.a.hx, body, ground)
    }
}
