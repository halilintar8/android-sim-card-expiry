package com.halilintar8.simexpiry.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.halilintar8.simexpiry.util.ReminderManager

/**
 * Receives the daily SIM expiry alarm and triggers background work.
 */
class SimExpiryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "Received broadcast: $action")

        if (!isExpectedAction(action)) {
            Log.w(TAG, "Unexpected broadcast ignored → $action")
            return
        }

        // Load reminderDays from Intent or SharedPreferences (via ReminderManager)
        val reminderDays = intent?.getIntExtra(EXTRA_REMINDER_DAYS, -1)
            ?.takeIf { it > 0 }
            ?: ReminderManager.getReminderDays(context)

        Log.i(TAG, "Running SIM expiry check → reminderDays=$reminderDays")

        // Run worker now
        enqueueWorker(context, reminderDays)

        // Reschedule the next daily alarm
        scheduleNextAlarm(context, reminderDays)
    }

    /**
     * Allow only broadcasts from AlarmScheduler or reboot flows.
     */
    private fun isExpectedAction(action: String?): Boolean {
        return action == null || action == ACTION_ALARM_TRIGGER
    }

    /**
     * Launch SimExpiryWorker immediately via WorkManager.
     */
    private fun enqueueWorker(context: Context, reminderDays: Int) {
        runCatching {
            val inputData = Data.Builder()
                .putInt(EXTRA_REMINDER_DAYS, reminderDays)
                .build()

            val request = OneTimeWorkRequestBuilder<SimExpiryWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "SimExpiryWorker enqueued with reminderDays=$reminderDays")
        }.onFailure {
            Log.e(TAG, "Failed to enqueue SimExpiryWorker", it)
        }
    }

    /**
     * Schedule tomorrow’s alarm at the target time.
     */
    private fun scheduleNextAlarm(context: Context, reminderDays: Int) {
        AlarmScheduler.scheduleDailyAlarm(
            context,
            TARGET_HOUR,
            TARGET_MINUTE,
            reminderDays
        )
        Log.d(TAG, "Next daily alarm scheduled for $TARGET_HOUR:$TARGET_MINUTE (reminderDays=$reminderDays)")
    }

    companion object {
        private const val TAG = "SimExpiryReceiver"

        // Expected alarm broadcast action
        const val ACTION_ALARM_TRIGGER = "com.halilintar8.simexpiry.ALARM_TRIGGER"

        // Default daily trigger time
        const val TARGET_HOUR = 7
        const val TARGET_MINUTE = 0

        // ReminderDays key
        const val EXTRA_REMINDER_DAYS = "reminder_days"
    }
}
