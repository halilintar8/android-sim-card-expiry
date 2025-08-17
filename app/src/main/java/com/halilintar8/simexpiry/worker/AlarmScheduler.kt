package com.halilintar8.simexpiry.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Schedule a daily alarm at the specified time, embedding reminderDays
     * and persisting it to SharedPreferences for fallback.
     */
    fun scheduleDailyAlarm(context: Context, hourOfDay: Int, minute: Int, reminderDays: Int) {
        // Save reminderDays in SharedPreferences so worker always knows last chosen value
        saveReminderDays(context, reminderDays)

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1) // schedule for tomorrow if time has passed
        }

        Log.d(
            TAG,
            "Scheduling daily alarm → ${next.time} (reminderDays=$reminderDays, target=$hourOfDay:$minute)"
        )
        scheduleExactAlarm(context, next.timeInMillis, reminderDays)
    }

    /**
     * Internal function: schedule an exact alarm, embedding reminderDays.
     */
    private fun scheduleExactAlarm(context: Context, triggerAtMillis: Long, reminderDays: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Android 12+ exact alarm permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(
                context,
                "Permission required to schedule exact alarms",
                Toast.LENGTH_LONG
            ).show()

            Log.w(TAG, "Cannot schedule exact alarm — opening system settings")
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        val intent = Intent(context, SimExpiryReceiver::class.java).apply {
            putExtra(SimExpiryReceiver.EXTRA_REMINDER_DAYS, reminderDays)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, // keep 0 since we only want a single daily alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(
                TAG,
                "Exact alarm scheduled successfully for ${
                    Calendar.getInstance().apply { timeInMillis = triggerAtMillis }.time
                } (reminderDays=$reminderDays)"
            )
        } catch (se: SecurityException) {
            Log.e(TAG, "Exact alarm scheduling failed: permission denied", se)
            Toast.makeText(context, "Exact alarm permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper: schedule alarm after X seconds (useful for testing).
     */
    fun scheduleExactAlarmInSeconds(context: Context, secondsFromNow: Int, reminderDays: Int) {
        saveReminderDays(context, reminderDays)
        val triggerTime = System.currentTimeMillis() + (secondsFromNow * 1000L)
        Log.d(
            TAG,
            "Scheduling test alarm in $secondsFromNow seconds (reminderDays=$reminderDays)"
        )
        scheduleExactAlarm(context, triggerTime, reminderDays)
    }

    /**
     * Persist reminderDays for fallback (so Worker can always read latest value).
     */
    private fun saveReminderDays(context: Context, reminderDays: Int) {
        val prefs = context.getSharedPreferences("sim_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("reminder_days", reminderDays).apply()
        Log.d(TAG, "ReminderDays saved to SharedPreferences → $reminderDays")
    }
}
