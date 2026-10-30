package com.sneha.ai.ui

/**
 * v2.1 — Mascot animation state mapping.
 *
 * Animation names here MUST match the animation names inside
 * app/src/main/assets/models/doreman.glb exactly (see CHANGELOG-2.1.md).
 * The GLB is node-animated (no skeleton): each animation targets real nodes
 * (root, head, eyes, mouth, arms).
 *
 * Transient states (happy, touch) are shown briefly and then return to the
 * base animation for the current Phase. They are driven by trigger counters
 * from HomeScreen, not by changing MainViewModel or the executor.
 */
enum class MascotTransient { NONE, HAPPY, TOUCH }

object MascotAnimations {
    const val IDLE = "idle"
    const val BLINK = "blink"
    const val LISTENING = "listening"
    const val SPEAKING = "speaking"
    const val THINKING = "thinking"
    const val HAPPY = "happy"
    const val TOUCH = "touch"

    val ALL = listOf(IDLE, BLINK, LISTENING, SPEAKING, THINKING, HAPPY, TOUCH)

    /** How long a transient animation is shown before returning to base. */
    const val HAPPY_MS = 2000L
    const val TOUCH_MS = 1500L

    /** Base (looping) animation for an assistant phase. */
    fun baseFor(phase: Phase): String = when (phase) {
        Phase.Idle -> IDLE
        Phase.Listening -> LISTENING
        Phase.Thinking -> THINKING
        Phase.Speaking -> SPEAKING
        Phase.Executing -> IDLE
    }

    /** Animation + whether it loops, for a transient or a base phase. */
    fun resolve(phase: Phase, transient: MascotTransient): Pair<String, Boolean> =
        when (transient) {
            MascotTransient.HAPPY -> HAPPY to false
            MascotTransient.TOUCH -> TOUCH to false
            MascotTransient.NONE -> baseFor(phase) to true
        }
}
