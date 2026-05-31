package com.example.buttonmapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val alarms = StorageHelper.loadScheduledAlarms(context)
            for (alarm in alarms) {
                AlarmScheduler.scheduleAlarm(context, alarm)
            }
        }
    }
}