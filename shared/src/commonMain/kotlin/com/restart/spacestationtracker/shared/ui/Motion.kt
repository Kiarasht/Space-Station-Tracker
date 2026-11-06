package com.restart.spacestationtracker.shared.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/** One duration for all page motion. */
const val PAGE_MOTION_MILLIS = 300

/** True when the system asks for reduced motion (Android animator scale 0, iOS Reduce Motion). */
@Composable
expect fun rememberReduceMotion(): Boolean

/** The incoming page: a short slide from the side of the bar it sits on, with a fade. */
fun tabEnterTransition(forward: Boolean, reduceMotion: Boolean): EnterTransition {
    if (reduceMotion) return EnterTransition.None
    val spec = tween<IntOffset>(PAGE_MOTION_MILLIS, easing = FastOutSlowInEasing)
    return slideInHorizontally(spec) { width -> if (forward) width / 3 else -width / 3 } +
        fadeIn(tween(PAGE_MOTION_MILLIS))
}

/** The outgoing page drifts a little the other way as it fades. */
fun tabExitTransition(forward: Boolean, reduceMotion: Boolean): ExitTransition {
    if (reduceMotion) return ExitTransition.None
    val spec = tween<IntOffset>(PAGE_MOTION_MILLIS, easing = FastOutSlowInEasing)
    return slideOutHorizontally(spec) { width -> if (forward) -width / 8 else width / 8 } +
        fadeOut(tween(PAGE_MOTION_MILLIS))
}

/**
 * Tab switches slide toward the tapped tab's side of the bar: a tab to the right comes in from the right.
 * [order] gives each tab's position in the bar.
 */
fun <T> directionalTabTransition(
    reduceMotion: Boolean,
    order: (T) -> Int
): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    val forward = order(targetState) >= order(initialState)
    tabEnterTransition(forward, reduceMotion) togetherWith tabExitTransition(forward, reduceMotion)
}
