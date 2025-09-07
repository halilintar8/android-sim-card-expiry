package com.halilintar8.simexpiry.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.halilintar8.simexpiry.data.SimCardDatabase
import com.halilintar8.simexpiry.util.NotificationHelper
import com.halilintar8.simexpiry.util.ReminderManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Worker to check SIM card expiry and trigger notifications.
 */
class SimExpiryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "SimExpiryWorker"

    override suspend fun doWork(): Result {
        val reminderDays = inputData.getInt(
            SimExpiryReceiver.EXTRA_REMINDER_DAYS,
            ReminderManager.getReminderDays(applicationContext)
        )

        Log.d(TAG, "Running SIM expiry check → reminderDays=$reminderDays")

        try {
            val simCardDao = SimCardDatabase.getDatabase(applicationContext).simCardDao()
            val simCards = simCardDao.getAllSimCardsList()

            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            simCards.forEach { sim ->
                val expiryDate = runCatching { LocalDate.parse(sim.expiredDate, formatter) }.getOrNull()
                if (expiryDate != null) {
                    val daysLeft = ChronoUnit.DAYS.between(today, expiryDate).toInt()
                    if (daysLeft in 0..reminderDays) {
                        NotificationHelper.showExpiryNotification(applicationContext, sim, daysLeft)
                        Log.d(TAG, "Notification sent for SIM=${sim.name} (daysLeft=$daysLeft)")
                    } else {
                        NotificationHelper.cancelNotification(applicationContext, sim)
                    }
                } else {
                    Log.w(TAG, "Invalid expiry date format for SIM=${sim.name}: ${sim.expiredDate}")
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running SIM expiry worker", e)
            return Result.failure()
        }
    }
}
