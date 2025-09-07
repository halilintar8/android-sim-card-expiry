package com.halilintar8.simexpiry.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    /**
     * Save the selected theme mode to SharedPreferences.
     * @param context Application context
     * @param mode AppCompatDelegate mode (MODE_NIGHT_NO / MODE_NIGHT_YES / MODE_NIGHT_FOLLOW_SYSTEM)
     */
    fun saveThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    /**
     * Load the saved theme mode from SharedPreferences.
     * Returns MODE_NIGHT_FOLLOW_SYSTEM if not set.
     */
    fun loadThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Apply the saved theme mode to the app immediately.
     */
    fun applySavedThemeMode(context: Context) {
        val mode = loadThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Toggle theme dynamically.
     * @param context Application context
     * @param mode The desired AppCompatDelegate mode
     */
    fun setThemeMode(context: Context, mode: Int) {
        saveThemeMode(context, mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
