package com.halilintar8.simexpiry.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object AlarmScheduler {

    fun scheduleDailyAlarm(context: Context, hourOfDay: Int, minute: Int) {
        val now = java.util.Calendar.getInstance()
        val next = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        scheduleExactAlarm(context, next.timeInMillis)
    }

    fun scheduleExactAlarm(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Android 12+ exact alarm permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    context,
                    "Permission required to schedule exact alarms",
                    Toast.LENGTH_LONG
                ).show()
                // Open settings so user can grant permission
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(context, SimExpiryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (se: SecurityException) {
            Toast.makeText(context, "Exact alarm permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun scheduleExactAlarmInSeconds(context: Context, secondsFromNow: Int) {
        val triggerTime = System.currentTimeMillis() + (secondsFromNow * 1000L)
        scheduleExactAlarm(context, triggerTime)
    }
}
