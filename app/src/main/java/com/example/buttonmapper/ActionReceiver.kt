package com.example.buttonmapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val actionName = intent.action
        if (actionName == "com.example.buttonmapper.HOURLY_VALIDATION") {
            ScheduleScheduler.validateAndExecute(context)
            ScheduleScheduler.startHourlyValidation(context, force = true)
        }
    }
}