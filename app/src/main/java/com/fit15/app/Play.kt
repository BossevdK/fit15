package com.fit15.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.ceil

private enum class Kind { READY, WARM, WORK, REST }

private class Seg(val kind: Kind, val ex: Exercise, val exNo: Int, val round: Int, val sec: Int)

private const val READY_SEC = 10

private const val WARM_SEC = 20

private fun buildPlan(w: Workout, intensity: Intensity, warmUp: Boolean): List<Seg> {
    val n = w.exercises.size
    val ex = w.exercises.map { it.forTier(intensity.tier) }
    val plan = mutableListOf(Seg(Kind.READY, if (warmUp) Exercises.warmUp[0] else ex[0], 1, 1, READY_SEC))
    if (warmUp) {
        for ((i, e) in Exercises.warmUp.withIndex()) plan += Seg(Kind.WARM, e, i + 1, 1, WARM_SEC)
        plan += Seg(Kind.REST, ex[0], 1, 1, intensity.restSec)
    }
    val total = n * w.rounds
    var count = 0
    for (r in 1..w.rounds) {
        for (i in 0 until n) {
            count++
            plan += Seg(Kind.WORK, ex[i], i + 1, r, intensity.workSec)
            if (count < total) {
                // rust: laat de volgende oefening zien
                val next = (i + 1) % n
                plan += Seg(Kind.REST, ex[next], next + 1, if (next == 0) r + 1 else r, intensity.restSec)
            }
        }
    }
    return plan
}

@Composable
fun PlayScreen(workout: Workout, intensity: Intensity, warmUp: Boolean, onQuit: () -> Unit, onFinish: (seconds: Int) -> Unit) {
    val plan = remember { buildPlan(workout, intensity, warmUp) }
    var idx by remember { mutableIntStateOf(0) }
    var remainingMs by remember { mutableLongStateOf(plan[0].sec * 1000L) }
    var paused by remember { mutableStateOf(false) }
    var confirmQuit by remember { mutableStateOf(false) }
    val totalSec = remember { plan.sumOf { it.sec } - READY_SEC }

    // scherm aan houden tijdens de workout
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

    BackHandler { paused = true; confirmQuit = true }

    fun advance() {
        if (idx + 1 < plan.size) {
            idx++
            remainingMs = plan[idx].sec * 1000L
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        } else {
            onFinish(totalSec)
        }
    }

    LaunchedEffect(idx, paused) {
        if (paused) return@LaunchedEffect
        var last = SystemClock.elapsedRealtime()
        var lastWhole = ceil(remainingMs / 1000.0).toInt()
        while (remainingMs > 0) {
            delay(100)
            val now = SystemClock.elapsedRealtime()
            remainingMs -= now - last
            last = now
            val whole = ceil(remainingMs / 1000.0).toInt()
            if (whole != lastWhole) {
                lastWhole = whole
                if (whole in 1..3) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            }
        }
        advance()
    }

    val seg = plan[idx]
    val c = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    // elke fase heeft een eigen kleurenpaar met genoeg contrast, in licht en donker
    val phaseColor = when (seg.kind) {
        Kind.WORK -> Signal
        Kind.REST -> if (dark) Color(0xFF4CC4B0) else Color(0xFF14786A)
        Kind.WARM -> if (dark) Color(0xFFF2B84B) else Color(0xFF8F6100)
        Kind.READY -> if (dark) Color(0xFFCDBFB4) else Color(0xFF65544A)
    }
    val onPhase = when {
        seg.kind == Kind.WORK -> Ink
        !dark -> Color.White
        seg.kind == Kind.READY -> Color(0xFF1A1210)
        seg.kind == Kind.REST -> Color(0xFF00201B)
        else -> Color(0xFF2A1D00)
    }
    val phaseLabel = when (seg.kind) {
        Kind.WORK -> "Work"
        Kind.REST -> "Rest"
        Kind.WARM -> "Warm-up"
        Kind.READY -> "Get ready"
    }
    val seconds = ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)
    val segProgress = (remainingMs.toFloat() / (seg.sec * 1000f)).coerceIn(0f, 1f)
    val workDone = plan.take(idx).count { it.kind == Kind.WORK }
    val workTotal = workout.exercises.size * workout.rounds

    val tint by animateColorAsState(lerp(c.background, phaseColor, 0.10f), tween(400), label = "tint")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(idx) {
        pop.snapTo(1.08f)
        pop.animateTo(1f, spring(dampingRatio = 0.5f))
    }
    val digitScale by animateFloatAsState(if (seconds in 1..3 && seg.kind != Kind.READY) 1.15f else 1f, label = "digits")

    Column(
        Modifier.fillMaxSize().background(tint).padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { paused = true; confirmQuit = true }, modifier = Modifier.height(48.dp)) {
                Text("✕ Stop", fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
            if (seg.kind == Kind.WARM) {
                Text("Warm-up ${seg.exNo}/${Exercises.warmUp.size}", fontSize = 14.sp, color = c.onSurfaceVariant)
            } else if (seg.kind != Kind.READY) {
                Text(
                    "Round ${seg.round}/${workout.rounds} · Exercise ${seg.exNo}/${workout.exercises.size}",
                    fontSize = 14.sp,
                    color = c.onSurfaceVariant,
                )
            }
        }
        // een streepje per oefening
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(workTotal) { i ->
                Box(
                    Modifier.weight(1f).height(5.dp).clip(CircleShape)
                        .background(if (i < workDone) c.primary else c.onSurface.copy(alpha = 0.12f)),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            phaseLabel,
            color = onPhase,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(phaseColor).padding(horizontal = 16.dp, vertical = 5.dp),
        )

        Spacer(Modifier.height(10.dp))
        FigureStage(seg.ex, Modifier.weight(1f).fillMaxWidth(), playing = !paused)
        Spacer(Modifier.height(10.dp))

        if (seg.kind == Kind.REST || seg.kind == Kind.READY) {
            Text("Next up", fontSize = 13.sp, color = c.onSurfaceVariant)
        }
        Text(seg.ex.name, style = Type.title, textAlign = TextAlign.Center)
        Text(
            if (seg.kind == Kind.WORK || seg.kind == Kind.WARM) seg.ex.tip else seg.ex.how,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).heightIn(min = 60.dp),
        )

        Box(
            Modifier
                .size(170.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .semantics { contentDescription = "$seconds seconds left" },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val inset = 12.dp.toPx()
                val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
                val topLeft = Offset(inset, inset)
                drawArc(phaseColor.copy(alpha = 0.15f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(phaseColor.copy(alpha = 0.18f), -90f, 360f * segProgress, false, topLeft, arcSize, style = Stroke(stroke + 8.dp.toPx(), cap = StrokeCap.Round))
                drawArc(phaseColor, -90f, 360f * segProgress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (segProgress > 0.01f) {
                    val ang = Math.toRadians((-90f + 360f * segProgress).toDouble())
                    val r = arcSize.width / 2f
                    val dot = Offset(size.width / 2f + r * kotlin.math.cos(ang).toFloat(), size.height / 2f + r * kotlin.math.sin(ang).toFloat())
                    drawCircle(onPhase, 4.dp.toPx(), dot)
                }
            }
            Text(
                seconds.toString(),
                style = Type.timer,
                modifier = Modifier.graphicsLayer { scaleX = digitScale; scaleY = digitScale },
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { paused = !paused },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(if (paused) "Resume" else "Pause", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { advance() },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Skip", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false; paused = false },
            title = { Text("Stop workout?") },
            text = { Text("Your progress for this session will not be saved.") },
            confirmButton = { TextButton(onClick = onQuit) { Text("Stop") } },
            dismissButton = { TextButton(onClick = { confirmQuit = false; paused = false }) { Text("Keep going") } },
        )
    }
}
