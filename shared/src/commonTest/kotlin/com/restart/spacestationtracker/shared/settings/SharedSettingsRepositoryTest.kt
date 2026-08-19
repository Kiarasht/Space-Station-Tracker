package com.restart.spacestationtracker.shared.settings

import com.restart.spacestationtracker.shared.preferences.PreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedSettingsRepositoryTest {

    @Test
    fun readsLegacyAndroidKeyNamesWithoutMigration() {
        val store = MemoryStore().apply {
            putBoolean("show_orbit", false)
            putString("map_type", "Satellite")
            putString("units", "Imperial")
            putBoolean("auto_pass_alerts_enabled", true)
            putDouble("auto_pass_alert_latitude", 34.05)
        }

        val settings = SharedSettingsRepository(store).settings.value

        assertFalse(settings.showOrbit)
        assertEquals("Satellite", settings.mapType)
        assertEquals("Imperial", settings.units)
        assertTrue(settings.automaticPassAlertsEnabled)
        assertEquals(34.05, settings.automaticPassAlertLatitude)
    }

    @Test
    fun writesUseTheFrozenStorageContract() {
        val store = MemoryStore()
        val repository = SharedSettingsRepository(store)

        repository.setShowOrbit(false)
        repository.setMapType("Hybrid")
        repository.setAutomaticPassAlertNotificationTimes(setOf("1 hour before"))

        assertEquals(false, store.getBoolean("show_orbit"))
        assertEquals("Hybrid", store.getString("map_type"))
        assertEquals(
            setOf("1 hour before"),
            store.getStringSet("auto_pass_alert_notification_times")
        )
    }
}

private class MemoryStore : PreferenceStore {
    private val values = mutableMapOf<String, Any>()

    override fun getInt(key: String): Int? = values[key] as? Int
    override fun putInt(key: String, value: Int) { values[key] = value }
    override fun getLong(key: String): Long? = values[key] as? Long
    override fun putLong(key: String, value: Long) { values[key] = value }
    override fun getBoolean(key: String): Boolean? = values[key] as? Boolean
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getString(key: String): String? = values[key] as? String
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getDouble(key: String): Double? = values[key] as? Double
    override fun putDouble(key: String, value: Double) { values[key] = value }
    override fun getStringSet(key: String): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return values[key] as? Set<String>
    }
    override fun putStringSet(key: String, value: Set<String>) {
        values[key] = value
    }
    override fun remove(key: String) {
        values.remove(key)
    }
}
