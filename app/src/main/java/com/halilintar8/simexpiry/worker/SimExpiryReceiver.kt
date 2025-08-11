package com.halilintar8.simexpiry.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar

class SimExpiryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast received: ${intent.action ?: "NO_ACTION"}")

        // Only handle our intended actions (boot or alarm trigger)
        if (!isExpectedAction(intent.action)) {
            Log.w(TAG, "Ignoring unexpected broadcast: ${intent.action}")
            return
        }

        // 1. Run the SIM expiry check immediately
        try {
            val workRequest = OneTimeWorkRequestBuilder<SimExpiryWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "SimExpiryWorker enqueued successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue SimExpiryWorker", e)
        }

        // 2. Schedule next run for tomorrow at the same time
        scheduleNextAlarm(context)
    }

    /**
     * Determines if the received broadcast is expected for SIM expiry checking.
     */
    private fun isExpectedAction(action: String?): Boolean {
        return action == null || // Direct sendBroadcast() from app may have no action
                action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_REBOOT
    }

    /**
     * Schedule the next exact alarm for tomorrow at TARGET_HOUR:TARGET_MINUTE.
     */
    private fun scheduleNextAlarm(context: Context) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, TARGET_HOUR)
            set(Calendar.MINUTE, TARGET_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // Always next day
        }

        AlarmScheduler.scheduleExactAlarm(context, calendar.timeInMillis)
        Log.d(TAG, "Next alarm scheduled for: ${calendar.time}")
    }

    companion object {
        private const val TAG = "SimExpiryReceiver"

        // Default daily trigger time
        const val TARGET_HOUR = 7   // 11 PM
        const val TARGET_MINUTE = 0 // :12 minutes
    }
}
