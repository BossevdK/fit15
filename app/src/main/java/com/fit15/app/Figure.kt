package com.fit15.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
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

class FigureColors(
    val skin: Color, val shirt: Color, val shorts: Color, val shoe: Color, val hair: Color, val ground: Color,
)

@Composable
fun figureColors(): FigureColors {
    val c = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    return FigureColors(
        skin = Color(0xFFE8B48C),
        shirt = c.primary,
        shorts = if (dark) Color(0xFF7C8CB3) else Color(0xFF34425E),
        shoe = c.onSurface,
        hair = if (dark) Color(0xFF1E140F) else Color(0xFF3B2A20),
        ground = c.onSurface.copy(alpha = 0.25f),
    )
}

private val Wood = Color(0xFFC8955A)
private val WoodDark = Color(0xFF9C6E3E)

private fun DrawScope.drawFigure(pose: Pose, prop: Prop, front: Boolean, anchorX: Float, colors: FigureColors) {
    val u = min(size.width / WORLD_W, size.height / WORLD_H)
    val ox = (size.width - WORLD_W * u) / 2f
    val oy = (size.height - WORLD_H * u) / 2f
    fun pt(o: Offset) = Offset(ox + o.x * u, oy + (WORLD_H - o.y) * u)
    fun line(a: Offset, b: Offset, c: Color, wd: Float) = drawLine(c, pt(a), pt(b), strokeWidth = wd * u, cap = StrokeCap.Round)
    val floorY = FLOOR - 1.3f

    // vloer: een dunne mat
    drawRoundRect(
        colors.ground.copy(alpha = 0.55f), pt(Offset(4f, floorY + 0.2f)), Size(92f * u, 1.6f * u), CornerRadius(0.8f * u),
    )

    // attributen (muur / stoel)
    when (prop) {
        Prop.WALL -> {
            val x = anchorX - 4.8f
            drawRect(colors.ground.copy(alpha = 0.3f), pt(Offset(x - 3f, 57f)), Size(3f * u, (57f - floorY) * u))
            for (y in listOf(14f, 26f, 38f, 50f)) line(Offset(x - 3f, y), Offset(x - 0.3f, y + 3f), colors.ground.copy(alpha = 0.5f), 0.5f)
        }
        Prop.CHAIR, Prop.CHAIR_FEET -> {
            val l = if (prop == Prop.CHAIR) anchorX - 11.5f else anchorX - 31f
            val r = if (prop == Prop.CHAIR) anchorX - 1.5f else anchorX - 19f
            val top = floorY + 10f
            line(Offset(l + 0.6f, top), Offset(l + 0.6f, floorY), WoodDark, 1.4f)
            line(Offset(r - 0.6f, top), Offset(r - 0.6f, floorY), WoodDark, 1.4f)
            line(Offset(l, top), Offset(r, top), Wood, 2.4f)
        }
        Prop.NONE -> Unit
    }

    val s = solve(pose)

    // schaduw onder de figuur (kleiner als je springt)
    val xs = listOf(s.hip.x, s.handA.x, s.handB.x, s.toeA.x, s.toeB.x, s.headC.x, s.kneeA.x)
    val cx = (xs.min() + xs.max()) / 2f
    val rx = (xs.max() - xs.min()) / 2f + 3f
    val shadow = (0.16f - pose.lift * 0.012f).coerceAtLeast(0.04f)
    drawOval(Color.Black.copy(alpha = shadow), pt(Offset(cx - rx, floorY + 0.4f)), Size(2 * rx * u, 2.4f * u))

    fun far(c: Color) = if (front) c else lerp(c, Color.Black, 0.28f)
    fun arm(elbow: Offset, hand: Offset, behind: Boolean) {
        line(elbow, hand, if (behind) far(colors.skin) else colors.skin, 2.3f)
        line(s.neck, elbow, if (behind) far(colors.shirt) else colors.shirt, 3.2f)
    }
    fun leg(knee: Offset, ankle: Offset, toe: Offset, behind: Boolean) {
        line(knee, ankle, if (behind) far(colors.skin) else colors.skin, 2.7f)
        line(ankle, toe, if (behind) far(colors.shoe) else colors.shoe, 2.5f)
        line(s.hip, knee, if (behind) far(colors.shorts) else colors.shorts, 4.0f)
    }

    // achterste ledematen, romp en hoofd, voorste ledematen
    arm(s.elbowB, s.handB, true)
    leg(s.kneeB, s.ankleB, s.toeB, true)
    line(s.hip, s.neck, colors.shirt, 5.0f)
    line(s.hip, s.hip + (s.neck - s.hip) * 0.32f, colors.shorts, 5.2f)
    line(s.neck, s.headC, colors.skin, 1.8f)
    val hc = pt(s.headC)
    val hr = HEAD_R * u
    drawCircle(colors.skin, hr, hc)
    val hairStart = if (front) 180f else 80f
    val hairSweep = if (front) 180f else 200f
    drawArc(colors.hair, hairStart, hairSweep, true, Offset(hc.x - hr, hc.y - hr), Size(2 * hr, 2 * hr))
    arm(s.elbowA, s.handA, false)
    leg(s.kneeA, s.ankleA, s.toeA, false)
}

/** Speelt een oefening af: gaat zacht heen en weer tussen twee houdingen. */
@Composable
fun ExerciseFigure(
    exercise: Exercise,
    modifier: Modifier,
    colors: FigureColors,
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
        drawFigure(lerpPose(exercise.a, exercise.b, e), exercise.prop, exercise.front, exercise.a.hx, colors)
    }
}

/** De figuur op een zachte kaart met een kleurverloop. */
@Composable
fun FigureStage(ex: Exercise, modifier: Modifier, playing: Boolean = true) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(lerp(c.surfaceVariant, c.primaryContainer, 0.35f), c.surfaceVariant)))
            .alpha(if (playing) 1f else 0.75f),
        contentAlignment = Alignment.Center,
    ) {
        ExerciseFigure(ex, Modifier.fillMaxSize().padding(6.dp), figureColors(), playing)
    }
}
