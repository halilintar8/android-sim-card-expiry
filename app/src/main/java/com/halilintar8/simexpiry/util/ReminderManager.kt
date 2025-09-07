package com.halilintar8.simexpiry.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Central place to read/write alarm time and reminder-days preferences.
 */
object ReminderManager {
    private const val TAG = "ReminderManager"

    private const val PREF_NAME = "sim_prefs" // <-- single canonical prefs name
    private const val KEY_ALARM_HOUR = "alarm_hour"
    private const val KEY_ALARM_MINUTE = "alarm_minute"
    private const val KEY_REMINDER_DAYS = "reminder_days"

    // App default values (7 days default as you asked)
    private const val DEFAULT_ALARM_HOUR = 7
    private const val DEFAULT_ALARM_MINUTE = 0
    private const val DEFAULT_REMINDER_DAYS = 7

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** Return Pair(hour, minute) of saved alarm time (or defaults). */
    fun getAlarmTime(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        val h = p.getInt(KEY_ALARM_HOUR, DEFAULT_ALARM_HOUR)
        val m = p.getInt(KEY_ALARM_MINUTE, DEFAULT_ALARM_MINUTE)
        Log.d(TAG, "getAlarmTime() -> $h:$m")
        return h to m
    }

    /** Save alarm time. */
    fun setAlarmTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit()
            .putInt(KEY_ALARM_HOUR, hour)
            .putInt(KEY_ALARM_MINUTE, minute)
            .apply()
        Log.i(TAG, "setAlarmTime() -> $hour:$minute")
    }

    /** Return days-before-expiry to notify (or default). */
    fun getReminderDays(context: Context): Int {
        val days = prefs(context).getInt(KEY_REMINDER_DAYS, DEFAULT_REMINDER_DAYS)
        Log.d(TAG, "getReminderDays() -> $days")
        return days
    }

    /** Save days-before-expiry to notify. Coerced to 1..365. */
    fun setReminderDays(context: Context, days: Int) {
        val safe = days.coerceIn(1, 365)
        prefs(context).edit()
            .putInt(KEY_REMINDER_DAYS, safe)
            .apply()
        Log.i(TAG, "setReminderDays() -> $safe")
    }
}
