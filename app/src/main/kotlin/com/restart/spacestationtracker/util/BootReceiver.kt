package com.restart.spacestationtracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.restart.spacestationtracker.data.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BootReceiverEntryPoint::class.java
                )
                val settings = entryPoint.settingsRepository().appSettingsFlow.first()
                if (settings.automaticPassAlertsEnabled) {
                    entryPoint.workScheduler().scheduleDailySync()
                    entryPoint.workScheduler().runImmediateSync()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun workScheduler(): AutomaticPassAlertWorkScheduler
}
