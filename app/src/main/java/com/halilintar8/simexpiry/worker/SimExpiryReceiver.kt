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
import com.halilintar8.simexpiry.data.SimCardDatabase
import com.halilintar8.simexpiry.util.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        if (action != ACTION_ALARM_TRIGGER) {
            Log.w(TAG, "Ignoring unexpected broadcast: $action")
            return
        }

        val extra = intent.getIntExtra(EXTRA_REMINDER_DAYS, -1)
        val reminderDays = if (extra > 0) extra else ReminderManager.getReminderDays(context)
        Log.i(TAG, "Alarm fired → reminderDays=$reminderDays")

        // Launch background work (Room DB query) on IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SimCardDatabase.getDatabase(context).simCardDao()
                val simCards = dao.getAllSimCardsList() // <-- need DAO with suspend fun returning List<SimCard>

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = Calendar.getInstance()

                val expiringSims = simCards.filter { sim ->
                    try {
                        val expiryCal = Calendar.getInstance().apply {
                            time = dateFormat.parse(sim.expiredDate) ?: return@filter false
                        }
                        val diffDays =
                            ((expiryCal.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                        diffDays in 0..reminderDays
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse date for SIM ${sim.name}", e)
                        false
                    }
                }

                if (expiringSims.isEmpty()) {
                    Log.i(TAG, "No SIMs expiring within $reminderDays days → no notification")
                    return@launch
                }

                val lines = expiringSims.map { sim ->
                    "${sim.name} (${sim.simCardNumber}) expires on ${sim.expiredDate}"
                }

                val contentText = if (lines.size == 1) {
                    lines[0]
                } else {
                    "${lines.size} SIM cards expiring soon"
                }

                val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("SIM Expiry Reminder")
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
                    .setAutoCancel(true)
                    .setContentIntent(AlarmScheduler.getOpenAppPendingIntent(context))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

                val nm = NotificationManagerCompat.from(context)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) nm.notify(NOTIFICATION_ID, builder.build())
                    else Log.w(TAG, "POST_NOTIFICATIONS not granted → skipping notify()")
                } else {
                    nm.notify(NOTIFICATION_ID, builder.build())
                }

                Log.d(TAG, "Notification posted with ${expiringSims.size} SIM(s)")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to build SIM expiry notification", t)
            }
        }
    }
}
