package com.example.myapplication.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.SimCardDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SimExpiryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "sim_expiry_channel"
        private const val NOTIFICATION_ID = 1001
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            createNotificationChannel()

            val dao = SimCardDatabase.getDatabase(applicationContext).simCardDao()
            val simCards = dao.getAllSimCardsList()

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val sevenDaysLater = (today.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 7)
            }

            val expiringOrExpiredCards = simCards.filter { card ->
                parseDate(card.expiredDate)?.let { expiryDate ->
                    expiryDate.before(today) ||
                            (!expiryDate.before(today) && !expiryDate.after(sevenDaysLater))
                } ?: false
            }

            if (expiringOrExpiredCards.isNotEmpty()) {
                val message = if (expiringOrExpiredCards.size == 1) {
                    val card = expiringOrExpiredCards.first()
                    "${card.name} (${card.simCardNumber}) expires on ${card.expiredDate}"
                } else {
                    "${expiringOrExpiredCards.size} SIM cards are expiring soon."
                }
                showNotification("SIM Card Expiry Warning", message)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
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
        } catch (_: Exception) {
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
            manager.createNotificationChannel(channel)
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
            .setAutoCancel(true) // ensures it's dismissed when clicked
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
