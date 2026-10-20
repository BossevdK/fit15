package com.fit15.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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

private val RestColor = Color(0xFF1B8A7A)

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
    val phaseColor = when (seg.kind) {
        Kind.WORK -> c.primary
        Kind.REST -> RestColor
        Kind.WARM -> Color(0xFFB07A00)
        Kind.READY -> c.onSurfaceVariant
    }
    val phaseLabel = when (seg.kind) {
        Kind.WORK -> "WORK"
        Kind.REST -> "REST"
        Kind.WARM -> "WARM-UP"
        Kind.READY -> "GET READY"
    }
    val seconds = ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)
    val segProgress = (remainingMs.toFloat() / (seg.sec * 1000f)).coerceIn(0f, 1f)
    val before = if (idx == 0) 0 else plan.take(idx).sumOf { it.sec } - READY_SEC
    val into = if (seg.kind == Kind.READY) 0 else (seg.sec - seconds).coerceAtLeast(0)
    val overall = ((before + into).toFloat() / totalSec).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { paused = true; confirmQuit = true }) { Text("✕ Stop", fontSize = 15.sp) }
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
        LinearProgressIndicator(
            progress = { overall },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = c.primary,
            trackColor = c.surfaceVariant,
        )

        Spacer(Modifier.height(12.dp))
        Text(
            phaseLabel,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(phaseColor).padding(horizontal = 16.dp, vertical = 5.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            ExerciseFigure(
                seg.ex,
                Modifier.fillMaxWidth().height(230.dp),
                body = c.onSurface,
                ground = c.onSurface.copy(alpha = 0.3f),
                playing = !paused,
            )
        }

        if (seg.kind == Kind.REST || seg.kind == Kind.READY) {
            Text("Next up", fontSize = 13.sp, color = c.onSurfaceVariant)
        }
        Text(seg.ex.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(
            if (seg.kind == Kind.WORK || seg.kind == Kind.WARM) seg.ex.tip else seg.ex.how,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).height(60.dp),
        )

        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            val track = c.surfaceVariant
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(phaseColor, -90f, 360f * segProgress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(seconds.toString(), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { paused = !paused },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (paused) "Resume" else "Pause", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { advance() },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
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
