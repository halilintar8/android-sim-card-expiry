package com.halilintar8.simexpiry.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.halilintar8.simexpiry.MainActivity
import com.halilintar8.simexpiry.R
import com.halilintar8.simexpiry.data.SimCardDatabase
import com.halilintar8.simexpiry.util.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Background worker that checks SIM card expiry dates
 * and shows notifications when SIMs are expired or close to expiry.
 */
class SimExpiryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SimExpiryWorker"
        private const val CHANNEL_ID = "sim_expiry_channel"
        private const val NOTIFICATION_ID = 1001
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            createNotificationChannel()

            // Get reminderDays from input or fallback to ReminderManager
            val reminderDays = inputData.getInt(SimExpiryReceiver.EXTRA_REMINDER_DAYS, -1)
                .takeIf { it > 0 }
                ?: ReminderManager.getReminderDays(applicationContext)

            Log.d(TAG, "Worker started with reminderDays=$reminderDays")

            val dao = SimCardDatabase.getDatabase(applicationContext).simCardDao()
            val simCards = dao.getAllSimCardsList()

            if (simCards.isEmpty()) {
                Log.d(TAG, "No SIM cards found in database → nothing to check")
                return@withContext Result.success()
            }

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val reminderLimit = (today.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, reminderDays)
            }

            val expiredCards = mutableListOf<String>()
            val expiringSoonCards = mutableListOf<String>()

            simCards.forEach { card ->
                parseDate(card.expiredDate)?.let { expiryDate ->
                    when {
                        expiryDate.before(today) -> {
                            expiredCards.add("${card.name} (${card.simCardNumber}) expired on ${card.expiredDate}")
                        }
                        expiryDate in today..reminderLimit -> {
                            expiringSoonCards.add("${card.name} (${card.simCardNumber}) expires on ${card.expiredDate}")
                        }
                    }
                }
            }

            // Notify expired SIMs
            if (expiredCards.isNotEmpty()) {
                val message = if (expiredCards.size == 1) {
                    expiredCards.first()
                } else {
                    "${expiredCards.size} SIM cards have already expired."
                }
                Log.i(TAG, "Expired SIMs → $message")
                showNotification("Expired SIM Alert", message)
            }

            // Notify expiring soon SIMs
            if (expiringSoonCards.isNotEmpty()) {
                val message = if (expiringSoonCards.size == 1) {
                    expiringSoonCards.first()
                } else {
                    "${expiringSoonCards.size} SIM cards are expiring within $reminderDays days."
                }
                Log.i(TAG, "Expiring soon SIMs → $message")
                showNotification("SIM Expiry Warning", message)
            }

            if (expiredCards.isEmpty() && expiringSoonCards.isEmpty()) {
                Log.d(TAG, "No SIM cards expiring or expired within $reminderDays days")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in worker", e)
            Result.failure()
        }
    }

    private fun parseDate(dateStr: String): Calendar? {
        return try {
            DATE_FORMAT.parse(dateStr)?.let {
                Calendar.getInstance().apply {
                    time = it
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse date: $dateStr", e)
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIM Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for SIM card expiry alerts"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
