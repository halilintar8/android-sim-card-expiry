package com.halilintar8.simexpiry.util

import android.content.Context
import android.util.Log

/**
 * Centralized manager for saving and loading reminder days.
 */
object ReminderManager {

    private const val PREFS_NAME = "sim_prefs"
    private const val KEY_REMINDER_DAYS = "reminder_days"
    private const val DEFAULT_REMINDER_DAYS = 7

    fun saveReminderDays(context: Context, days: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REMINDER_DAYS, days)
            .apply()
        Log.d("ReminderManager", "Saved reminderDays=$days")
    }

    fun getReminderDays(context: Context): Int {
        val days = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REMINDER_DAYS, DEFAULT_REMINDER_DAYS)
        Log.d("ReminderManager", "Loaded reminderDays=$days")
        return days
    }
}
