package com.halilintar8.simexpiry.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.halilintar8.simexpiry.util.ReminderManager

class BootReceiver : BroadcastReceiver() {
    companion object { private const val TAG = "BootReceiver" }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_REBOOT ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Boot detected - rescheduling alarm")
            val (hour, minute) = ReminderManager.getAlarmTime(context)
            AlarmScheduler.scheduleDailyAlarm(context, hour, minute)
        }
    }
}
