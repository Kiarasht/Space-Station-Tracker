package com.restart.spacestationtracker.util

import java.util.concurrent.atomic.AtomicBoolean

private var isRunningTest: AtomicBoolean? = null

@Synchronized
fun isRunningTest(): Boolean {
    if (isRunningTest == null) {
        val isTest: Boolean = try {
            Class.forName ("android.support.test.espresso.Espresso");
            true
        } catch (e: ClassNotFoundException) {
            false
        }
        isRunningTest = AtomicBoolean(isTest)
    }
    return isRunningTest?.get() ?: false
}
