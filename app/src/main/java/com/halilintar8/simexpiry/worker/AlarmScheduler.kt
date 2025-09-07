package com.halilintar8.simexpiry.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.halilintar8.simexpiry.util.ReminderManager
import java.util.*

/**
 * Schedule/cancel daily alarm. This implementation reads reminderDays
 * from ReminderManager so callers only pass alarm time (hour, minute).
 */
object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderDays = ReminderManager.getReminderDays(context)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, SimExpiryReceiver::class.java).apply {
            action = SimExpiryReceiver.ACTION_ALARM_TRIGGER
            putExtra(SimExpiryReceiver.EXTRA_REMINDER_DAYS, reminderDays)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                Log.w(TAG, "Exact alarms may not be permitted by OS/user")
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.i(TAG, "scheduleDailyAlarm: scheduled ${hour}:${minute} (reminderDays=$reminderDays)")
        } catch (se: SecurityException) {
            Log.e(TAG, "scheduleDailyAlarm: SecurityException, falling back to set()", se)
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelDailyAlarm(context: Context) {
        val intent = Intent(context, SimExpiryReceiver::class.java).apply {
            action = SimExpiryReceiver.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "cancelDailyAlarm: alarm cancelled")
    }

    /** Returns a PendingIntent that opens the app when a notification is tapped. */
    fun getOpenAppPendingIntent(context: Context): PendingIntent {
        val open = Intent(context, com.halilintar8.simexpiry.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
