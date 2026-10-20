package com.fit15.app

import android.content.Context
import java.time.LocalDate

/** Bewaart alles lokaal op de telefoon (geen account, geen server). */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("fit15", Context.MODE_PRIVATE)

    class Entry(val day: Long, val workoutId: String, val minutes: Int)

    var level: Level
        get() = runCatching { Level.valueOf(prefs.getString("level", null) ?: "MEDIUM") }.getOrDefault(Level.MEDIUM)
        set(value) {
            prefs.edit().putString("level", value.name).apply()
        }

    /** Hoeveel trainingen van het 4-wekenprogramma al klaar zijn. */
    var programDone: Int
        get() = prefs.getInt("programDone", 0)
        set(value) {
            prefs.edit().putInt("programDone", value).apply()
        }

    var warmUp: Boolean
        get() = prefs.getBoolean("warmUp", true)
        set(value) {
            prefs.edit().putBoolean("warmUp", value).apply()
        }

    var reminderOn: Boolean
        get() = prefs.getBoolean("reminderOn", false)
        set(value) {
            prefs.edit().putBoolean("reminderOn", value).apply()
        }

    var reminderHour: Int
        get() = prefs.getInt("reminderHour", 18)
        set(value) {
            prefs.edit().putInt("reminderHour", value).apply()
        }

    var reminderMinute: Int
        get() = prefs.getInt("reminderMinute", 0)
        set(value) {
            prefs.edit().putInt("reminderMinute", value).apply()
        }

    var soundOn: Boolean
        get() = prefs.getBoolean("soundOn", true)
        set(value) {
            prefs.edit().putBoolean("soundOn", value).apply()
        }

    var voiceOn: Boolean
        get() = prefs.getBoolean("voiceOn", false)
        set(value) {
            prefs.edit().putBoolean("voiceOn", value).apply()
        }

    var vibrateOn: Boolean
        get() = prefs.getBoolean("vibrateOn", true)
        set(value) {
            prefs.edit().putBoolean("vibrateOn", value).apply()
        }

    /** Hoeveel trainingen je per week wilt doen. */
    var weeklyGoal: Int
        get() = prefs.getInt("weeklyGoal", 3)
        set(value) {
            prefs.edit().putInt("weeklyGoal", value.coerceIn(1, 7)).apply()
        }

    fun history(): List<Entry> =
        (prefs.getString("history", "") ?: "").split(";").mapNotNull { s ->
            val p = s.split("|")
            if (p.size == 3) runCatching { Entry(p[0].toLong(), p[1], p[2].toInt()) }.getOrNull() else null
        }

    fun add(workoutId: String, minutes: Int) {
        val old = prefs.getString("history", "") ?: ""
        val entry = "${LocalDate.now().toEpochDay()}|$workoutId|$minutes"
        prefs.edit().putString("history", if (old.isEmpty()) entry else "$old;$entry").apply()
    }
}

/** Aantal dagen op rij met minstens één workout (vandaag mag nog ontbreken). */
fun streakOf(history: List<Store.Entry>): Int {
    val days = history.map { it.day }.toSet()
    val today = LocalDate.now().toEpochDay()
    var d = if (today in days) today else today - 1
    var n = 0
    while (d in days) {
        n++
        d--
    }
    return n
}

/** Sets per spiergroep in de afgelopen 7 dagen. Hoofdspieren tellen voor 1 set, hulpspieren voor een halve. */
fun weeklySets(history: List<Store.Entry>): Map<Muscle, Float> {
    val since = LocalDate.now().toEpochDay() - 6
    val result = mutableMapOf<Muscle, Float>()
    for (e in history) {
        if (e.day < since) continue
        val w = Workouts.byId(e.workoutId) ?: continue
        for (ex in w.exercises) {
            for (m in ex.primary) result[m] = (result[m] ?: 0f) + w.rounds
            for (m in ex.secondary) result[m] = (result[m] ?: 0f) + w.rounds * 0.5f
        }
    }
    return result
}

/** Langste reeks dagen op rij met minstens één workout. */
fun bestStreak(history: List<Store.Entry>): Int {
    val days = history.map { it.day }.toSortedSet().toList()
    var best = 0
    var run = 0
    var prev = Long.MIN_VALUE
    for (d in days) {
        run = if (d == prev + 1) run + 1 else 1
        if (run > best) best = run
        prev = d
    }
    return best
}

/** Aantal workouts sinds maandag van deze week. */
fun thisWeekCount(history: List<Store.Entry>): Int {
    val monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toEpochDay()
    return history.count { it.day >= monday }
}
