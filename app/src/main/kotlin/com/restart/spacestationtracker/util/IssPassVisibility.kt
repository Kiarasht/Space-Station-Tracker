package com.restart.spacestationtracker.util

import androidx.annotation.StringRes
import com.restart.spacestationtracker.R

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

    @StringRes
    fun labelResForVisibility(visibility: String): Int {
        return when (visibility) {
            VERY_BRIGHT -> R.string.visibility_very_bright
            BRIGHT -> R.string.visibility_bright
            MODERATE -> R.string.visibility_moderate
            FAINT -> R.string.visibility_faint
            else -> R.string.visibility_very_faint
        }
    }

    @StringRes
    fun labelResForMagnitude(magnitude: Double): Int {
        return when {
            magnitude < -2.0 -> R.string.visibility_very_bright
            magnitude < -1.5 -> R.string.visibility_bright
            magnitude < -1.0 -> R.string.visibility_moderate
            magnitude < 0.0 -> R.string.visibility_faint
            else -> R.string.visibility_very_faint
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
