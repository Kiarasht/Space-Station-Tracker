package com.restart.spacestationtracker.shared.ui

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberReduceMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
