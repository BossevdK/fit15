package com.fit15.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun ChoiceRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Signal else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(if (selected) Signal else Color.Transparent)
                .border(1.5.dp, if (selected) Signal else MaterialTheme.colorScheme.outline, CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = Type.row)
            Text(sub, style = Type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Drie korte vragen en een samenvatting. Duurt een halve minuut. */
@Composable
fun OnboardingScreen(onFinish: (Goal, Experience, Int) -> Unit, onSkip: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableStateOf(Goal.MUSCLE) }
    var exp by remember { mutableStateOf(Experience.NEW) }
    var days by remember { mutableIntStateOf(3) }
    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPad, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.weight(1f).height(5.dp).clip(CircleShape)
                        .background(if (i <= step) Signal else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) {
                Text("‹ Back", style = Type.row, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { step-- }.padding(vertical = 12.dp))
            }
            Spacer(Modifier.weight(1f))
            if (step < 3) {
                Text("Skip", style = Type.row, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onSkip).padding(vertical = 12.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (step) {
                0 -> {
                    Text("What is your goal?", style = Type.title)
                    Text("Fit15 picks the right workouts for it.", style = Type.body, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    for (g in Goal.entries) ChoiceRow(g.label, g.sub, g == goal) { goal = g }
                }
                1 -> {
                    Text("How much do you train now?", style = Type.title)
                    Text("This sets your first week and how hard it feels.", style = Type.body, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    for (e in Experience.entries) ChoiceRow(e.label, e.sub, e == exp) { exp = e }
                }
                2 -> {
                    Text("How many days a week?", style = Type.title)
                    Text("Leave a rest day between sessions so your muscles can grow.", style = Type.body, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    for (n in 2..5) {
                        ChoiceRow(
                            "$n days a week",
                            when (n) {
                                2 -> "Easy to keep up. Two full workouts a week."
                                3 -> "The classic: train, rest, train, rest."
                                4 -> "Adds a 7-minute workout to each week."
                                else -> "Adds two 7-minute workouts to each week."
                            },
                            n == days,
                        ) { days = n }
                    }
                }
                else -> {
                    val weeks = Program.build(goal, days)
                    val week = weeks[exp.startWeek]
                    Text("Your plan is ready", style = Type.title)
                    Text(
                        "${weeks.size} weeks, ${weeks[0].sessions.size} sessions a week, ${goal.label.lowercase()}.",
                        style = Type.body,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "You start in week ${exp.startWeek + 1}, ${week.title.lowercase()}. " +
                            "First up: ${Workouts.byId(week.sessions[0])?.name}.",
                        style = Type.body,
                    )
                    Text(
                        "Your weekly goal is $days workouts and the pace is ${exp.level.label.lowercase()}. " +
                            "You can change all of this later in Settings.",
                        style = Type.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SignalButton(
            when (step) {
                2 -> "Show my plan"
                3 -> "Start training"
                else -> "Next"
            },
        ) {
            if (step < 3) step++ else onFinish(goal, exp, days)
        }
    }
}
