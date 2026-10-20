package com.fit15.app

enum class Muscle(val label: String) {
    CHEST("Chest"), SHOULDERS("Shoulders"), TRICEPS("Triceps"), CORE("Core"), UPPER_BACK("Upper back"),
    LOWER_BACK("Lower back"), GLUTES("Glutes"), QUADS("Quads"), HAMSTRINGS("Hamstrings"), CALVES("Calves"),
}

class Exercise(
    val id: String,
    val name: String,
    val muscles: String,
    val how: String,
    val tip: String,
    val a: Pose,
    val b: Pose,
    val primary: List<Muscle>,
    val secondary: List<Muscle> = emptyList(),
    val halfPeriodMs: Int = 1200,
    val prop: Prop = Prop.NONE,
    val front: Boolean = false,
    val easier: Exercise? = null,
    val harder: Exercise? = null,
) {
    /** Easy kiest de makkelijkere variant (als die er is), Hard de zwaardere. */
    fun forTier(tier: Int): Exercise = when {
        tier < 0 -> easier ?: this
        tier > 0 -> harder ?: this
        else -> this
    }
}

class Workout(
    val id: String,
    val name: String,
    val focus: String,
    val exercises: List<Exercise>,
    val rounds: Int = 3,
) {
    /** Elke oefening duurt (werk + rust) = 60 seconden, dus minuten = oefeningen x rondes. */
    val minutes: Int get() = exercises.size * rounds
}

/** Werktijd, rusttijd en welke variant (-1 makkelijker, 0 normaal, +1 zwaarder). */
class Intensity(val workSec: Int, val restSec: Int, val tier: Int) {
    val hint: String get() = "$workSec s on, $restSec s rest"
}

enum class Level(val label: String, val intensity: Intensity) {
    EASY("Easy", Intensity(30, 30, -1)),
    MEDIUM("Medium", Intensity(40, 20, 0)),
    HARD("Hard", Intensity(45, 15, 1)),
}

object Exercises {
    // ---------- varianten (eerst, want de gewone oefeningen verwijzen ernaar) ----------
    private val halfSquat = Exercise(
        id = "halfsquat", name = "Half squat", muscles = "Quads, glutes",
        how = "Same as a squat, but only go down about halfway. A gentler way to build strength.",
        tip = "Keep your heels on the floor and your chest up.",
        a = Pose(hx = 50f, torso = 90f),
        b = Pose(hx = 43.2f, torso = 75f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f, thA = -40f, shA = -95f, thB = -40f, shB = -95f),
        primary = listOf(Muscle.QUADS, Muscle.GLUTES), secondary = listOf(Muscle.HAMSTRINGS),
        halfPeriodMs = 1200,
    )

    private val jumpSquat = Exercise(
        id = "jumpsquat", name = "Jump squat", muscles = "Quads, glutes, calves",
        how = "Squat down, then explode upward into a jump. Land softly and go straight into the next squat.",
        tip = "Swing your arms to jump higher. Land with soft knees.",
        a = Pose(hx = 41.9f, torso = 65f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f, thA = -10f, shA = -100f, thB = -10f, shB = -100f),
        b = Pose(hx = 50f, torso = 90f, uaA = 20f, faA = 40f, uaB = 20f, faB = 40f, toeA = 60f, toeB = 60f, lift = 4f),
        primary = listOf(Muscle.QUADS, Muscle.GLUTES, Muscle.CALVES), secondary = listOf(Muscle.HAMSTRINGS, Muscle.CORE),
        halfPeriodMs = 550,
    )

    private val kneePushUp = Exercise(
        id = "kneepushup", name = "Knee push-up", muscles = "Chest, triceps",
        how = "Like a push-up, but with your knees on the floor. Keep a straight line from your knees to your head.",
        tip = "Lower your chest all the way to the floor. Move to full push-ups when this feels easy.",
        a = Pose(
            hx = 40f, torso = 40f, uaA = 270f, faA = 270f, uaB = 270f, faB = 270f,
            thA = 220f, shA = 165f, thB = 220f, shB = 165f, toeA = -60f, toeB = -60f,
        ),
        b = Pose(
            hx = 41.9f, torso = 15.5f, uaA = 177.9f, faA = 294.4f, uaB = 177.9f, faB = 294.4f,
            thA = 195.5f, shA = 165f, thB = 195.5f, shB = 165f, toeA = -60f, toeB = -60f,
        ),
        primary = listOf(Muscle.CHEST, Muscle.TRICEPS), secondary = listOf(Muscle.SHOULDERS, Muscle.CORE),
        halfPeriodMs = 1400,
    )

    private val declinePushUp = Exercise(
        id = "declinepushup", name = "Decline push-up", muscles = "Upper chest, shoulders, triceps",
        how = "Put your feet on a chair or sofa and do a push-up. The higher your feet, the harder it gets.",
        tip = "Keep your core tight so your hips do not sag.",
        a = Pose(
            hx = 42f, torso = 4.4f, thA = 184.4f, shA = 184.4f, thB = 184.4f, shB = 184.4f, toeA = 75f, toeB = 75f,
        ),
        b = Pose(
            hx = 41.6f, torso = -10.8f, uaA = 210.9f, faA = 338.8f, uaB = 210.9f, faB = 338.8f,
            thA = 169.2f, shA = 169.2f, thB = 169.2f, shB = 169.2f, toeA = 75f, toeB = 75f,
        ),
        primary = listOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS), secondary = listOf(Muscle.CORE),
        halfPeriodMs = 1500, prop = Prop.CHAIR_FEET,
    )

    private val slowClimber = Exercise(
        id = "slowclimber", name = "Slow mountain climber", muscles = "Core, shoulders",
        how = "From a high plank, bring one knee toward your chest at a controlled pace, then switch.",
        tip = "Slow it down and focus on squeezing your abs.",
        a = Pose(hx = 40.5f, torso = 22.4f, thA = -20f, shA = 200f, thB = 202.4f, shB = 202.4f, toeA = 75f, toeB = 75f),
        b = Pose(hx = 40.5f, torso = 22.4f, thA = 202.4f, shA = 202.4f, thB = -20f, shB = 200f, toeA = 75f, toeB = 75f),
        primary = listOf(Muscle.CORE), secondary = listOf(Muscle.SHOULDERS, Muscle.QUADS),
        halfPeriodMs = 900,
    )

    private val plankLegLift = Exercise(
        id = "plankleg", name = "Plank leg lift", muscles = "Core, glutes, shoulders",
        how = "Hold a forearm plank and lift one leg up behind you. Lower it and switch legs every rep.",
        tip = "Do not twist your hips. Squeeze the glute of the lifted leg.",
        a = Pose(hx = 39.8f, torso = 7.8f, uaA = -90f, faA = 0f, uaB = -90f, faB = 0f, thA = 187.8f, shA = 187.8f, thB = 187.8f, shB = 187.8f, toeA = 75f, toeB = 75f),
        b = Pose(hx = 39.8f, torso = 7.8f, uaA = -90f, faA = 0f, uaB = -90f, faB = 0f, thA = 168f, shA = 168f, thB = 187.8f, shB = 187.8f, toeA = 20f, toeB = 75f),
        primary = listOf(Muscle.CORE, Muscle.GLUTES), secondary = listOf(Muscle.SHOULDERS),
        halfPeriodMs = 1300,
    )

    private val singleLegBridge = Exercise(
        id = "slbridge", name = "Single-leg glute bridge", muscles = "Glutes, hamstrings",
        how = "Do a glute bridge with one leg held straight up. Keep your hips level. Switch legs halfway.",
        tip = "Push through the heel of the leg on the floor.",
        a = Pose(
            hx = 56f, torso = 180f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 60f, shA = -60.5f, thB = 60f, shB = -60.5f, headDir = 150f,
        ),
        b = Pose(
            hx = 53.4f, torso = 202f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 25f, shA = -70f, thB = 60f, shB = 60f, headDir = 150f,
        ),
        primary = listOf(Muscle.GLUTES, Muscle.HAMSTRINGS), secondary = listOf(Muscle.CORE, Muscle.LOWER_BACK),
        halfPeriodMs = 1500,
    )

    private val bentKneeRaise = Exercise(
        id = "kneeraise", name = "Bent-knee raise", muscles = "Lower abs",
        how = "Lie on your back and bring your knees up to a tabletop position, then lower your feet back toward the floor.",
        tip = "Keep your lower back pressed into the floor.",
        a = Pose(
            hx = 36f, torso = 180f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 4f, shA = 4f, thB = 4f, shB = 4f, toeA = -80f, toeB = -80f, headDir = 150f,
        ),
        b = Pose(
            hx = 36f, torso = 180f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 90f, shA = 0f, thB = 90f, shB = 0f, toeA = -80f, toeB = -80f, headDir = 150f,
        ),
        primary = listOf(Muscle.CORE),
        halfPeriodMs = 1500,
    )

    private val singleLegCalf = Exercise(
        id = "slcalf", name = "Single-leg calf raise", muscles = "Calves",
        how = "Stand on one foot and rise up onto your toes, then lower slowly. Switch legs halfway.",
        tip = "Hold a wall for balance. Go slow on the way down.",
        a = Pose(hx = 50f, torso = 90f, thB = -90f, shB = 160f),
        b = Pose(hx = 50f, torso = 90f, thB = -90f, shB = 160f, toeA = 70f),
        primary = listOf(Muscle.CALVES),
        halfPeriodMs = 1100,
    )

    // ---------- gewone oefeningen ----------
    val squat = Exercise(
        id = "squat", name = "Squat", muscles = "Quads, glutes",
        how = "Feet shoulder-width apart. Push your hips back and bend your knees until your thighs are parallel to the floor, then stand up.",
        tip = "Take 3 seconds going down, then drive up fast. Slow lowering builds muscle.",
        a = Pose(hx = 50f, torso = 90f),
        b = Pose(hx = 41.9f, torso = 65f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f, thA = -10f, shA = -100f, thB = -10f, shB = -100f),
        primary = listOf(Muscle.QUADS, Muscle.GLUTES), secondary = listOf(Muscle.HAMSTRINGS, Muscle.CORE),
        halfPeriodMs = 1500, easier = halfSquat, harder = jumpSquat,
    )

    val pushUp = Exercise(
        id = "pushup", name = "Push-up", muscles = "Chest, shoulders, triceps",
        how = "Hands under your shoulders, body in one straight line. Lower your chest to the floor, then press back up.",
        tip = "Too hard? Do them on your knees. Squeeze your glutes to keep your back flat.",
        a = Pose(hx = 40.5f, torso = 22.4f, thA = 202.4f, shA = 202.4f, thB = 202.4f, shB = 202.4f, toeA = 75f, toeB = 75f),
        b = Pose(
            hx = 41.9f, torso = 6.1f, uaA = 188.6f, faA = 313.5f, uaB = 188.6f, faB = 313.5f,
            thA = 186.1f, shA = 186.1f, thB = 186.1f, shB = 186.1f, toeA = 75f, toeB = 75f,
        ),
        primary = listOf(Muscle.CHEST, Muscle.TRICEPS), secondary = listOf(Muscle.SHOULDERS, Muscle.CORE),
        halfPeriodMs = 1400, easier = kneePushUp, harder = declinePushUp,
    )

    val reverseLunge = Exercise(
        id = "lunge", name = "Reverse lunge", muscles = "Quads, glutes, hamstrings",
        how = "Step one foot back and lower until both knees are bent about 90 degrees. Push through your front heel to stand, then switch legs.",
        tip = "Keep your chest tall and your front knee above your ankle.",
        a = Pose(hx = 50f, torso = 90f),
        b = Pose(hx = 40.3f, torso = 90f, thA = -15f, shA = -90f, thB = -100f, shB = 178f, toeA = 0f, toeB = 60f),
        primary = listOf(Muscle.QUADS, Muscle.GLUTES), secondary = listOf(Muscle.HAMSTRINGS, Muscle.CALVES),
        halfPeriodMs = 1500,
    )

    val plank = Exercise(
        id = "plank", name = "Plank", muscles = "Core, shoulders",
        how = "Forearms on the floor, elbows under your shoulders. Hold your body in a straight line from head to heels.",
        tip = "Squeeze your abs and glutes and breathe. Do not let your hips sag.",
        a = Pose(hx = 39.8f, torso = 7.8f, uaA = -90f, faA = 0f, uaB = -90f, faB = 0f, thA = 187.8f, shA = 187.8f, thB = 187.8f, shB = 187.8f, toeA = 75f, toeB = 75f),
        b = Pose(hx = 39.8f, torso = 7.8f, uaA = -90f, faA = 0f, uaB = -90f, faB = 0f, thA = 187.8f, shA = 187.8f, thB = 187.8f, shB = 187.8f, toeA = 75f, toeB = 75f),
        primary = listOf(Muscle.CORE), secondary = listOf(Muscle.SHOULDERS, Muscle.GLUTES),
        harder = plankLegLift,
    )

    val mountainClimber = Exercise(
        id = "climber", name = "Mountain climber", muscles = "Core, shoulders, legs",
        how = "Start in a high plank. Drive one knee toward your chest, then quickly switch legs as if you are running.",
        tip = "Keep your hips low and your hands directly under your shoulders.",
        a = slowClimber.a, b = slowClimber.b,
        primary = listOf(Muscle.CORE), secondary = listOf(Muscle.SHOULDERS, Muscle.QUADS),
        halfPeriodMs = 450, easier = slowClimber,
    )

    val gluteBridge = Exercise(
        id = "bridge", name = "Glute bridge", muscles = "Glutes, hamstrings",
        how = "Lie on your back with your knees bent and feet flat. Push through your heels and lift your hips until your body is a straight line from shoulders to knees.",
        tip = "Squeeze your glutes hard for one second at the top.",
        a = singleLegBridge.a, b = Pose(
            hx = 53.4f, torso = 202f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 25f, shA = -70f, thB = 25f, shB = -70f, headDir = 150f,
        ),
        primary = listOf(Muscle.GLUTES, Muscle.HAMSTRINGS), secondary = listOf(Muscle.LOWER_BACK, Muscle.CORE),
        halfPeriodMs = 1400, harder = singleLegBridge,
    )

    val crunch = Exercise(
        id = "crunch", name = "Crunch", muscles = "Abs",
        how = "Lie on your back with your knees bent. Curl your shoulders off the floor toward your knees, then lower slowly.",
        tip = "Do not pull on your neck. Think about bringing your ribs toward your hips.",
        a = Pose(
            hx = 56f, torso = 180f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 60f, shA = -60.5f, thB = 60f, shB = -60.5f, headDir = 150f,
        ),
        b = Pose(
            hx = 56f, torso = 125f, uaA = -10f, faA = -10f, uaB = -10f, faB = -10f,
            thA = 60f, shA = -60.5f, thB = 60f, shB = -60.5f, headDir = 120f,
        ),
        primary = listOf(Muscle.CORE),
        halfPeriodMs = 1200,
    )

    val legRaise = Exercise(
        id = "legraise", name = "Leg raise", muscles = "Lower abs, hip flexors",
        how = "Lie on your back with your legs straight. Raise them until they point at the ceiling, then lower them slowly without touching the floor.",
        tip = "Press your lower back into the floor. Bend your knees a little if it is too hard.",
        a = Pose(
            hx = 36f, torso = 180f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 3f, shA = 3f, thB = 3f, shB = 3f, toeA = -80f, toeB = -80f, headDir = 150f,
        ),
        b = Pose(
            hx = 36f, torso = 180f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 88f, shA = 88f, thB = 88f, shB = 88f, toeA = -80f, toeB = -80f, headDir = 150f,
        ),
        primary = listOf(Muscle.CORE), secondary = listOf(Muscle.QUADS),
        halfPeriodMs = 1800, easier = bentKneeRaise,
    )

    val superman = Exercise(
        id = "superman", name = "Superman", muscles = "Lower back, glutes, shoulders",
        how = "Lie face down with your arms stretched forward. Lift your arms, chest and legs off the floor at the same time and hold for a second.",
        tip = "Look at the floor to keep your neck relaxed. Lift with your back, not your neck.",
        a = Pose(
            hx = 45f, torso = 0f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 180f, shA = 180f, thB = 180f, shB = 180f, toeA = 170f, toeB = 170f, headDir = 30f,
        ),
        b = Pose(
            hx = 45f, torso = 8f, uaA = 22f, faA = 22f, uaB = 22f, faB = 22f,
            thA = 168f, shA = 168f, thB = 168f, shB = 168f, toeA = 170f, toeB = 170f, headDir = 30f,
        ),
        primary = listOf(Muscle.LOWER_BACK, Muscle.UPPER_BACK), secondary = listOf(Muscle.GLUTES, Muscle.SHOULDERS),
        halfPeriodMs = 1500,
    )

    val jumpingJack = Exercise(
        id = "jack", name = "Jumping jack", muscles = "Full body, cardio",
        how = "Jump your feet apart while swinging your arms overhead, then jump back to the start.",
        tip = "Land softly on the balls of your feet.",
        a = Pose(
            hx = 50f, torso = 90f, uaA = -80f, faA = -80f, uaB = -100f, faB = -100f,
            thA = -92f, shA = -92f, thB = -88f, shB = -88f, toeA = 0f, toeB = 180f,
        ),
        b = Pose(
            hx = 50f, torso = 90f, uaA = 40f, faA = 80f, uaB = 140f, faB = 100f,
            thA = -62f, shA = -62f, thB = -118f, shB = -118f, toeA = 0f, toeB = 180f, lift = 2.5f,
        ),
        primary = listOf(Muscle.CALVES, Muscle.SHOULDERS), secondary = listOf(Muscle.QUADS, Muscle.GLUTES),
        halfPeriodMs = 380, front = true,
    )

    val highKnees = Exercise(
        id = "highknees", name = "High knees", muscles = "Legs, core, cardio",
        how = "Run on the spot and drive your knees up to hip height. Swing your arms with each step.",
        tip = "Stay tall and keep your steps quick and light.",
        a = Pose(
            hx = 50f, torso = 92f, uaA = 200f, faA = 120f, uaB = -30f, faB = 60f,
            thA = 5f, shA = -85f, thB = -90f, shB = -90f,
        ),
        b = Pose(
            hx = 50f, torso = 92f, uaA = -30f, faA = 60f, uaB = 200f, faB = 120f,
            thA = -90f, shA = -90f, thB = 5f, shB = -85f,
        ),
        primary = listOf(Muscle.QUADS, Muscle.CORE), secondary = listOf(Muscle.CALVES),
        halfPeriodMs = 320,
    )

    val wallSit = Exercise(
        id = "wallsit", name = "Wall sit", muscles = "Quads, glutes",
        how = "Lean your back against a wall and slide down until your knees are bent at 90 degrees. Hold the position.",
        tip = "Keep your knees above your ankles and press your whole back into the wall.",
        a = Pose(hx = 45f, torso = 90f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f, thA = 0f, shA = -90f, thB = 0f, shB = -90f),
        b = Pose(hx = 45f, torso = 90f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f, thA = 0f, shA = -90f, thB = 0f, shB = -90f),
        primary = listOf(Muscle.QUADS), secondary = listOf(Muscle.GLUTES, Muscle.CALVES),
        prop = Prop.WALL,
    )

    val calfRaise = Exercise(
        id = "calf", name = "Calf raise", muscles = "Calves",
        how = "Stand tall and rise up onto your toes as high as you can, then lower slowly.",
        tip = "Pause for a second at the top. Hold a wall for balance if needed.",
        a = Pose(hx = 50f, torso = 90f),
        b = Pose(hx = 50f, torso = 90f, toeA = 70f, toeB = 70f),
        primary = listOf(Muscle.CALVES),
        halfPeriodMs = 1000, harder = singleLegCalf,
    )

    val tricepDip = Exercise(
        id = "dip", name = "Chair dip", muscles = "Triceps, shoulders",
        how = "Sit on the edge of a sturdy chair with your hands next to your hips. Slide off the seat, lower by bending your elbows, then press back up.",
        tip = "Keep your back close to the chair. Bend your knees more to make it easier.",
        a = Pose(
            hx = 45f, torso = 100f, uaA = 270f, faA = 270f, uaB = 270f, faB = 270f,
            thA = -37.6f, shA = -37.6f, thB = -37.6f, shB = -37.6f, toeA = -60f, toeB = -60f, headDir = 95f,
        ),
        b = Pose(
            hx = 45f, torso = 100f, uaA = 210f, faA = 330f, uaB = 210f, faB = 330f,
            thA = -12.1f, shA = -12.1f, thB = -12.1f, shB = -12.1f, toeA = -60f, toeB = -60f, headDir = 95f,
        ),
        primary = listOf(Muscle.TRICEPS), secondary = listOf(Muscle.SHOULDERS, Muscle.CHEST),
        halfPeriodMs = 1400, prop = Prop.CHAIR,
    )

    val bicycleCrunch = Exercise(
        id = "bicycle", name = "Bicycle crunch", muscles = "Abs, obliques",
        how = "Lie on your back with your hands behind your head. Bring one knee in while the other leg stretches out, and alternate as if you are pedaling.",
        tip = "Slow and controlled beats fast. Rotate your chest, not just your elbows.",
        a = Pose(
            hx = 56f, torso = 140f, uaA = 120f, faA = -40f, uaB = 120f, faB = -40f,
            thA = 75f, shA = -5f, thB = 20f, shB = 20f, toeA = -80f, toeB = -80f, headDir = 115f,
        ),
        b = Pose(
            hx = 56f, torso = 140f, uaA = 120f, faA = -40f, uaB = 120f, faB = -40f,
            thA = 20f, shA = 20f, thB = 75f, shB = -5f, toeA = -80f, toeB = -80f, headDir = 115f,
        ),
        primary = listOf(Muscle.CORE), secondary = listOf(Muscle.QUADS),
        halfPeriodMs = 700,
    )

    val flutterKicks = Exercise(
        id = "flutter", name = "Flutter kicks", muscles = "Lower abs, hip flexors",
        how = "Lie on your back with your legs straight and a few centimetres off the floor. Kick your legs up and down in small, quick movements.",
        tip = "Press your lower back into the floor the whole time.",
        a = Pose(
            hx = 36f, torso = 175f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 18f, shA = 18f, thB = 4f, shB = 4f, toeA = -80f, toeB = -80f, headDir = 150f,
        ),
        b = Pose(
            hx = 36f, torso = 175f, uaA = 0f, faA = 0f, uaB = 0f, faB = 0f,
            thA = 4f, shA = 4f, thB = 18f, shB = 18f, toeA = -80f, toeB = -80f, headDir = 150f,
        ),
        primary = listOf(Muscle.CORE), secondary = listOf(Muscle.QUADS),
        halfPeriodMs = 280,
    )

    val donkeyKick = Exercise(
        id = "donkey", name = "Donkey kick", muscles = "Glutes, hamstrings",
        how = "Start on hands and knees. Kick one leg straight back and up, squeeze your glute, then lower it. Switch legs halfway.",
        tip = "Keep your hips square to the floor. Do not arch your lower back.",
        a = Pose(
            hx = 45f, torso = 25f, uaA = 270f, faA = 270f, uaB = 270f, faB = 270f,
            thA = -90f, shA = 180f, thB = -90f, shB = 180f, toeA = 180f, toeB = 180f,
        ),
        b = Pose(
            hx = 45f, torso = 25f, uaA = 270f, faA = 270f, uaB = 270f, faB = 270f,
            thA = 165f, shA = 165f, thB = -90f, shB = 180f, toeA = 180f, toeB = 180f,
        ),
        primary = listOf(Muscle.GLUTES), secondary = listOf(Muscle.HAMSTRINGS, Muscle.CORE),
        halfPeriodMs = 1100,
    )

    val birdDog = Exercise(
        id = "birddog", name = "Bird dog", muscles = "Core, lower back, glutes",
        how = "Start on hands and knees. Reach one arm forward and the opposite leg back at the same time, hold for a second, then switch sides.",
        tip = "Keep your back flat like a tabletop and do not rock your hips.",
        a = Pose(
            hx = 45f, torso = 25f, uaA = 270f, faA = 270f, uaB = 270f, faB = 270f,
            thA = -90f, shA = 180f, thB = -90f, shB = 180f, toeA = 180f, toeB = 180f,
        ),
        b = Pose(
            hx = 45f, torso = 25f, uaA = 15f, faA = 15f, uaB = 270f, faB = 270f,
            thA = -90f, shA = 180f, thB = 172f, shB = 172f, toeA = 180f, toeB = 180f,
        ),
        primary = listOf(Muscle.CORE, Muscle.LOWER_BACK), secondary = listOf(Muscle.GLUTES, Muscle.SHOULDERS),
        halfPeriodMs = 1500,
    )

    // ---------- warming-up (niet in de lijst met workouts) ----------
    val armCircles = Exercise(
        id = "armcircles", name = "Arm circles", muscles = "Shoulders",
        how = "Stand tall and swing your arms in big circles.",
        tip = "Start small and make the circles bigger.",
        a = Pose(
            hx = 50f, torso = 90f, uaA = 45f, faA = 45f, uaB = 135f, faB = 135f,
            thA = -80f, shA = -80f, thB = -100f, shB = -100f, toeA = 0f, toeB = 180f,
        ),
        b = Pose(
            hx = 50f, torso = 90f, uaA = -45f, faA = -45f, uaB = 225f, faB = 225f,
            thA = -80f, shA = -80f, thB = -100f, shB = -100f, toeA = 0f, toeB = 180f,
        ),
        primary = listOf(Muscle.SHOULDERS), halfPeriodMs = 700, front = true,
    )

    val marchInPlace = Exercise(
        id = "march", name = "March in place", muscles = "Legs, hips",
        how = "March on the spot, lifting your knees and swinging your arms.",
        tip = "Get your heart rate up gently.",
        a = Pose(
            hx = 50f, torso = 92f, uaA = 200f, faA = 120f, uaB = -30f, faB = 60f,
            thA = -20f, shA = -95f, thB = -90f, shB = -90f,
        ),
        b = Pose(
            hx = 50f, torso = 92f, uaA = -30f, faA = 60f, uaB = 200f, faB = 120f,
            thA = -90f, shA = -90f, thB = -20f, shB = -95f,
        ),
        primary = listOf(Muscle.QUADS), halfPeriodMs = 450,
    )

    val warmUp: List<Exercise> = listOf(armCircles, marchInPlace, halfSquat)
}

class Week(val title: String, val intensity: Intensity, val sessions: List<String>)

/** 4 weken, 3 trainingen per week, elke week iets zwaarder. */
object Program {
    const val NAME = "4-Week Builder"

    val weeks = listOf(
        Week("Learn the moves", Intensity(30, 30, -1), listOf("full", "legs", "core")),
        Week("Build", Intensity(40, 20, 0), listOf("upper", "legs", "core")),
        Week("Push harder", Intensity(45, 15, 0), listOf("full2", "glute", "core2")),
        Week("Peak week", Intensity(45, 15, 1), listOf("upper", "legs", "full")),
    )

    val total: Int get() = weeks.sumOf { it.sessions.size }

    class Next(val weekIndex: Int, val sessionIndex: Int, val week: Week, val workout: Workout)

    fun next(done: Int): Next? {
        if (done >= total) return null
        var left = done
        for ((wi, w) in weeks.withIndex()) {
            if (left < w.sessions.size) return Next(wi, left, w, Workouts.byId(w.sessions[left])!!)
            left -= w.sessions.size
        }
        return null
    }
}

object Workouts {
    private val e = Exercises

    val full = Workout("full", "Full Body Blast", "Whole body", listOf(e.squat, e.pushUp, e.reverseLunge, e.plank, e.mountainClimber))
    val upper = Workout("upper", "Upper Body Builder", "Chest, arms, shoulders", listOf(e.pushUp, e.tricepDip, e.superman, e.plank, e.mountainClimber))
    val legs = Workout("legs", "Legs & Glutes", "Legs, glutes, calves", listOf(e.squat, e.reverseLunge, e.gluteBridge, e.wallSit, e.calfRaise))
    val core = Workout("core", "Core Crusher", "Abs, lower back", listOf(e.crunch, e.legRaise, e.plank, e.mountainClimber, e.superman))
    val full2 = Workout("full2", "Strength Mix", "Whole body", listOf(e.reverseLunge, e.pushUp, e.gluteBridge, e.tricepDip, e.legRaise))
    val cardio = Workout("cardio", "Sweat Session", "Cardio and legs", listOf(e.jumpingJack, e.highKnees, e.squat, e.mountainClimber, e.pushUp))
    val glute = Workout("glute", "Glute Builder", "Glutes, hamstrings", listOf(e.squat, e.donkeyKick, e.gluteBridge, e.reverseLunge, e.wallSit))
    val core2 = Workout("core2", "Abs & Stability", "Abs, lower back", listOf(e.bicycleCrunch, e.flutterKicks, e.plank, e.birdDog, e.legRaise))

    val quickFull = Workout(
        "quickfull", "Quick 7: Full Body", "Whole body in 7 minutes",
        listOf(e.squat, e.pushUp, e.reverseLunge, e.plank, e.gluteBridge, e.mountainClimber, e.crunch), rounds = 1,
    )
    val quickAbs = Workout(
        "quickabs", "Quick 7: Abs", "Core in 7 minutes",
        listOf(e.crunch, e.bicycleCrunch, e.legRaise, e.plank, e.flutterKicks, e.birdDog, e.superman), rounds = 1,
    )

    val main = listOf(full, upper, legs, core, full2, glute, core2, cardio)
    val quick = listOf(quickFull, quickAbs)
    val all = main + quick

    fun byId(id: String) = all.firstOrNull { it.id == id }
}
