package com.fit15.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(applicationContext)
        setContent { Fit15Theme { App(store) } }
    }
}

@Composable
fun Fit15Theme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFFF8A5C), onPrimary = Color(0xFF2B1000),
            primaryContainer = Color(0xFF4A2A1C), onPrimaryContainer = Color(0xFFFFDBCB),
            tertiary = Color(0xFF4CC4B0), onTertiary = Color(0xFF00201B),
            background = Color(0xFF14110F), onBackground = Color(0xFFEDE4DC),
            surface = Color(0xFF1E1A17), onSurface = Color(0xFFEDE4DC),
            surfaceVariant = Color(0xFF2A2420), onSurfaceVariant = Color(0xFFCDBFB4),
            secondaryContainer = Color(0xFF4A2A1C), onSecondaryContainer = Color(0xFFFFDBCB),
            outline = Color(0xFF8C7F75), outlineVariant = Color(0xFF3A322D),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFD63D00), onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDBCB), onPrimaryContainer = Color(0xFF3B1300),
            tertiary = Color(0xFF14786A), onTertiary = Color.White,
            background = Color(0xFFFFF8F3), onBackground = Color(0xFF1F1612),
            surface = Color.White, onSurface = Color(0xFF1F1612),
            surfaceVariant = Color(0xFFF7ECE4), onSurfaceVariant = Color(0xFF65544A),
            secondaryContainer = Color(0xFFFFDBCB), onSecondaryContainer = Color(0xFF3B1300),
            outline = Color(0xFF8A766B), outlineVariant = Color(0xFFEBDDD2),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

sealed interface Screen {
    data object Home : Screen
    data object Muscles : Screen
    data class Detail(val workout: Workout, val fromProgram: Boolean) : Screen
    data class Play(val workout: Workout, val intensity: Intensity, val warmUp: Boolean, val fromProgram: Boolean) : Screen
    data class Done(val workout: Workout, val seconds: Int, val fromProgram: Boolean) : Screen
}

@Composable
fun App(store: Store) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var level by remember { mutableStateOf(store.level) }
    var warmUp by remember { mutableStateOf(store.warmUp) }
    var programDone by remember { mutableIntStateOf(store.programDone) }
    var refresh by remember { mutableIntStateOf(0) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    // kijk bij het openen of er een nieuwere versie op GitHub staat
    LaunchedEffect(Unit) {
        update = runCatching { withContext(Dispatchers.IO) { checkForUpdate(currentVersion(ctx)) } }.getOrNull()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            when (val s = screen) {
                Screen.Home -> HomeScreen(
                    store = store,
                    history = remember(refresh) { store.history() },
                    programDone = programDone,
                    update = update,
                    onOpen = { screen = Screen.Detail(it, false) },
                    onProgram = { Program.next(programDone)?.let { n -> screen = Screen.Detail(n.workout, true) } },
                    onRestartProgram = { store.programDone = 0; programDone = 0 },
                    onMuscles = { screen = Screen.Muscles },
                )
                Screen.Muscles -> {
                    BackHandler { screen = Screen.Home }
                    MusclesScreen(history = remember(refresh) { store.history() }, onBack = { screen = Screen.Home })
                }
                is Screen.Detail -> {
                    BackHandler { screen = Screen.Home }
                    DetailScreen(
                        workout = s.workout,
                        level = level,
                        onLevel = { level = it; store.level = it },
                        program = if (s.fromProgram) Program.next(programDone) else null,
                        warmUp = warmUp,
                        onWarmUp = { warmUp = it; store.warmUp = it },
                        onBack = { screen = Screen.Home },
                        onStart = { screen = Screen.Play(s.workout, it, warmUp, s.fromProgram) },
                    )
                }
                is Screen.Play -> PlayScreen(
                    workout = s.workout,
                    intensity = s.intensity,
                    warmUp = s.warmUp,
                    onQuit = { screen = Screen.Detail(s.workout, s.fromProgram) },
                    onFinish = { seconds ->
                        store.add(s.workout.id, (seconds + 30) / 60)
                        if (s.fromProgram) {
                            store.programDone = store.programDone + 1
                            programDone = store.programDone
                        }
                        refresh++
                        screen = Screen.Done(s.workout, seconds, s.fromProgram)
                    },
                )
                is Screen.Done -> {
                    BackHandler { screen = Screen.Home }
                    DoneScreen(
                        workout = s.workout,
                        seconds = s.seconds,
                        streak = streakOf(store.history()),
                        note = if (s.fromProgram) "Program: $programDone of ${Program.total} sessions done" else null,
                        onDone = { screen = Screen.Home },
                    )
                }
            }
        }
    }
}
