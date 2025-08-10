package com.example.myapplication.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // Reschedule the daily alarm after reboot
            AlarmScheduler.scheduleDailyAlarm(
                context,
                SimExpiryReceiver.TARGET_HOUR,
                SimExpiryReceiver.TARGET_MINUTE
            )
        }
    }
}
