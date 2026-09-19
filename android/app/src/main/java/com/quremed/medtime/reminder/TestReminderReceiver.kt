package com.quremed.medtime.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TestReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderSoundService.stop(context)
    }
}
