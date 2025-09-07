package com.halilintar8.simexpiry.worker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.halilintar8.simexpiry.R
import com.halilintar8.simexpiry.util.ReminderManager

class SimExpiryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.halilintar8.simexpiry.ACTION_ALARM_TRIGGER"
        const val EXTRA_REMINDER_DAYS = "extra_reminder_days"

        private const val TAG = "SimExpiryReceiver"
        private const val NOTIFICATION_CHANNEL_ID = "sim_expiry_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "onReceive action=$action")

        if (action != ACTION_ALARM_TRIGGER) {
            Log.w(TAG, "Ignoring unexpected broadcast: $action")
            return
        }

        // Prefer the extra passed in the alarm Intent, fallback to ReminderManager
        val extra = intent.getIntExtra(EXTRA_REMINDER_DAYS, -1)
        val reminderDays = if (extra > 0) extra else ReminderManager.getReminderDays(context)
        Log.i(TAG, "Alarm fired → reminderDays=$reminderDays")

        // Build the notification
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SIM Expiry Reminder")
            .setContentText("SIM(s) will expire in $reminderDays day(s).")
            .setAutoCancel(true)
            .setContentIntent(AlarmScheduler.getOpenAppPendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val nm = NotificationManagerCompat.from(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    nm.notify(NOTIFICATION_ID, builder.build())
                    Log.d(TAG, "Notification posted (API 33+)")
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS not granted → skipping notify()")
                }
            } else {
                // Pre-Android 13: no runtime permission needed
                nm.notify(NOTIFICATION_ID, builder.build())
                Log.d(TAG, "Notification posted (pre-API 33)")
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException when posting notification", se)
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error when showing notification", t)
        }
    }
}
