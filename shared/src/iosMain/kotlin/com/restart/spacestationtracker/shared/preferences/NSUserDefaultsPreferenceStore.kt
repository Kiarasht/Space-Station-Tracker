package com.restart.spacestationtracker.shared.preferences

import platform.Foundation.NSUserDefaults

class NSUserDefaultsPreferenceStore(
    suiteName: String
) : PreferenceStore {
    private val userDefaults = NSUserDefaults(suiteName = suiteName)

    override fun getInt(key: String): Int? {
        return if (userDefaults.objectForKey(key) == null) {
            null
        } else {
            userDefaults.integerForKey(key).toInt()
        }
    }

    override fun putInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getLong(key: String): Long? {
        return if (userDefaults.objectForKey(key) == null) {
            null
        } else {
            userDefaults.integerForKey(key)
        }
    }

    override fun putLong(key: String, value: Long) {
        userDefaults.setInteger(value, forKey = key)
    }

    override fun getBoolean(key: String): Boolean? {
        return if (userDefaults.objectForKey(key) == null) {
            null
        } else {
            userDefaults.boolForKey(key)
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, forKey = key)
    }

    override fun getString(key: String): String? = userDefaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }

    override fun getDouble(key: String): Double? {
        return if (userDefaults.objectForKey(key) == null) {
            null
        } else {
            userDefaults.doubleForKey(key)
        }
    }

    override fun putDouble(key: String, value: Double) {
        userDefaults.setDouble(value, forKey = key)
    }

    override fun getStringSet(key: String): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return (userDefaults.stringArrayForKey(key) as? List<String>)?.toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        userDefaults.setObject(value.toList(), forKey = key)
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }
}
