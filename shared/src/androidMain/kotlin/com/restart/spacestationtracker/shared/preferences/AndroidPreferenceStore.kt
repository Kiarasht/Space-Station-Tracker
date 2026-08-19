package com.restart.spacestationtracker.shared.preferences

import android.content.Context

class AndroidPreferenceStore(
    context: Context,
    name: String
) : PreferenceStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getInt(key: String): Int? {
        return if (preferences.contains(key)) preferences.getInt(key, 0) else null
    }

    override fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    override fun getLong(key: String): Long? {
        return if (preferences.contains(key)) preferences.getLong(key, 0L) else null
    }

    override fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    override fun getBoolean(key: String): Boolean? {
        return if (preferences.contains(key)) preferences.getBoolean(key, false) else null
    }

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getDouble(key: String): Double? {
        return if (preferences.contains(key)) {
            Double.fromBits(preferences.getLong(key, 0L))
        } else {
            null
        }
    }

    override fun putDouble(key: String, value: Double) {
        preferences.edit().putLong(key, value.toBits()).apply()
    }

    override fun getStringSet(key: String): Set<String>? {
        return preferences.getStringSet(key, null)?.toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        preferences.edit().putStringSet(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
