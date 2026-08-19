package com.restart.spacestationtracker.shared.passes

enum class PassVisibility {
    VERY_BRIGHT,
    BRIGHT,
    MODERATE,
    FAINT,
    VERY_FAINT;

    companion object {
        fun fromMagnitude(magnitude: Double): PassVisibility {
            return when {
                magnitude < -2.0 -> VERY_BRIGHT
                magnitude < -1.5 -> BRIGHT
                magnitude < -1.0 -> MODERATE
                magnitude < 0.0 -> FAINT
                else -> VERY_FAINT
            }
        }
    }
}
