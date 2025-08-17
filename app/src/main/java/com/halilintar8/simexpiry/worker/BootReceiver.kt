package com.halilintar8.simexpiry.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.halilintar8.simexpiry.util.ReminderManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received broadcast: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_REBOOT -> {
                Log.i(TAG, "Boot/Reboot completed → rescheduling daily SIM expiry alarm")

                val prefs = context.getSharedPreferences("sim_prefs", Context.MODE_PRIVATE)

                val hour = prefs.getInt("alarm_hour", SimExpiryReceiver.TARGET_HOUR)
                val minute = prefs.getInt("alarm_minute", SimExpiryReceiver.TARGET_MINUTE)
                val reminderDays = ReminderManager.getReminderDays(context)

                Log.d(TAG, "Restored alarm settings → hour=$hour, minute=$minute, reminderDays=$reminderDays")

                AlarmScheduler.scheduleDailyAlarm(context, hour, minute, reminderDays)
            }

            else -> Log.w(TAG, "Ignoring unrelated broadcast: $action")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
