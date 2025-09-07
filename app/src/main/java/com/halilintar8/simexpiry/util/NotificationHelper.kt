package com.halilintar8.simexpiry.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.halilintar8.simexpiry.R
import com.halilintar8.simexpiry.data.SimCard

/**
 * Helper for showing and managing SIM expiry notifications.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "sim_expiry_channel"

    /**
     * Show a notification for a SIM card nearing expiry.
     * Safely checks permission and handles potential exceptions.
     */
    fun showExpiryNotification(context: Context, simCard: SimCard, daysLeft: Int) {
        // Check POST_NOTIFICATIONS permission (Android 13+)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        val title = "SIM Expiry Reminder"
        val message = if (daysLeft > 0) {
            "SIM ${simCard.name} (${simCard.simCardNumber}) will expire in $daysLeft day(s)."
        } else {
            "SIM ${simCard.name} (${simCard.simCardNumber}) expires today!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(simCard.id, builder.build())
            Log.d(TAG, "Notification shown for SIM ${simCard.name} (ID: ${simCard.id})")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show notification due to permission/security issue", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }

    /**
     * Cancel a notification for a specific SIM card.
     */
    fun cancelNotification(context: Context, simCard: SimCard) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(simCard.id)
            Log.d(TAG, "Notification canceled for SIM ${simCard.name} (ID: ${simCard.id})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification", e)
        }
    }

    /**
     * Cancel all SIM expiry notifications.
     */
    fun cancelAllNotifications(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancelAll()
            Log.d(TAG, "All notifications canceled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel all notifications", e)
        }
    }
}
