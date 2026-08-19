package com.restart.spacestationtracker.util

import androidx.annotation.StringRes
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.shared.passes.PassAlertPolicy
import com.restart.spacestationtracker.shared.passes.PassVisibility

object IssPassVisibility {
    const val FAINT = "Faint"
    const val MODERATE = "Moderate"
    const val BRIGHT = "Bright"
    const val VERY_BRIGHT = "Very Bright"

    val options = listOf(FAINT, MODERATE, BRIGHT, VERY_BRIGHT)

    fun labelForMagnitude(magnitude: Double): String {
        return when (PassVisibility.fromMagnitude(magnitude)) {
            PassVisibility.VERY_BRIGHT -> VERY_BRIGHT
            PassVisibility.BRIGHT -> BRIGHT
            PassVisibility.MODERATE -> MODERATE
            PassVisibility.FAINT -> FAINT
            PassVisibility.VERY_FAINT -> "Very Faint"
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
        return when (PassVisibility.fromMagnitude(magnitude)) {
            PassVisibility.VERY_BRIGHT -> R.string.visibility_very_bright
            PassVisibility.BRIGHT -> R.string.visibility_bright
            PassVisibility.MODERATE -> R.string.visibility_moderate
            PassVisibility.FAINT -> R.string.visibility_faint
            PassVisibility.VERY_FAINT -> R.string.visibility_very_faint
        }
    }

    fun matchesMinimum(magnitude: Double, minimumVisibility: String): Boolean {
        return PassAlertPolicy.matchesMinimumVisibility(magnitude, minimumVisibility)
    }
}
