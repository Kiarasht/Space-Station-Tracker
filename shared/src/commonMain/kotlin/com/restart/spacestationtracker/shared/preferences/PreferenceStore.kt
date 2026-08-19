package com.restart.spacestationtracker.shared.preferences

interface PreferenceStore {
    fun getInt(key: String): Int?
    fun putInt(key: String, value: Int)
    fun getLong(key: String): Long?
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String): Boolean?
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String): String? = null
    fun putString(key: String, value: String) = Unit
    fun getDouble(key: String): Double? = null
    fun putDouble(key: String, value: Double) = Unit
    fun getStringSet(key: String): Set<String>? = null
    fun putStringSet(key: String, value: Set<String>) = Unit
    fun remove(key: String) = Unit
}
