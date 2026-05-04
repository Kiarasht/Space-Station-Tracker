package com.restart.spacestationtracker.util

object IssPassVisibility {
    const val FAINT = "Faint"
    const val MODERATE = "Moderate"
    const val BRIGHT = "Bright"
    const val VERY_BRIGHT = "Very Bright"

    val options = listOf(FAINT, MODERATE, BRIGHT, VERY_BRIGHT)

    fun labelForMagnitude(magnitude: Double): String {
        return when {
            magnitude < -2.0 -> VERY_BRIGHT
            magnitude < -1.5 -> BRIGHT
            magnitude < -1.0 -> MODERATE
            magnitude < 0.0 -> FAINT
            else -> "Very Faint"
        }
    }

    fun matchesMinimum(magnitude: Double, minimumVisibility: String): Boolean {
        return magnitude < thresholdFor(minimumVisibility)
    }

    private fun thresholdFor(minimumVisibility: String): Double {
        return when (minimumVisibility) {
            VERY_BRIGHT -> -2.0
            BRIGHT -> -1.5
            MODERATE -> -1.0
            FAINT -> 0.0
            else -> -1.5
        }
    }
}
