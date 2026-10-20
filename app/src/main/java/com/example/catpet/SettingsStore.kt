package com.example.catpet

import android.content.Context

/**
 * App-level settings. Uses its own SharedPreferences namespace ("catpet_settings")
 * and never touches the note / todo / screenshot record stores.
 */
class SettingsStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /** "left" or "right". */
    var defaultDockSide: String
        get() = preferences.getString(KEY_DEFAULT_DOCK_SIDE, DOCK_LEFT) ?: DOCK_LEFT
        set(value) {
            preferences.edit().putString(KEY_DEFAULT_DOCK_SIDE, value).apply()
        }

    var snapToEdge: Boolean
        get() = preferences.getBoolean(KEY_SNAP_TO_EDGE, true)
        set(value) {
            preferences.edit().putBoolean(KEY_SNAP_TO_EDGE, value).apply()
        }

    var remindersEnabled: Boolean
        get() = preferences.getBoolean(KEY_REMINDERS_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()
        }

    /** Persisted dock state so a service restart can try to restore it. */
    var docked: Boolean
        get() = preferences.getBoolean(KEY_DOCKED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_DOCKED, value).apply()
        }

    var dockSide: String
        get() = preferences.getString(KEY_DOCK_SIDE, defaultDockSide) ?: defaultDockSide
        set(value) {
            preferences.edit().putString(KEY_DOCK_SIDE, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "catpet_settings"
        private const val KEY_DEFAULT_DOCK_SIDE = "default_dock_side"
        private const val KEY_SNAP_TO_EDGE = "snap_to_edge"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val KEY_DOCKED = "docked"
        private const val KEY_DOCK_SIDE = "dock_side"

        const val DOCK_LEFT = "left"
        const val DOCK_RIGHT = "right"
    }
}
