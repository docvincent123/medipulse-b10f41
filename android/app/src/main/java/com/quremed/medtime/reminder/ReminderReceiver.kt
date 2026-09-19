package com.quremed.medtime.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.quremed.medtime.data.AppStore
import com.quremed.medtime.data.PendingReminder

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICATION_ID) ?: return
        val store = AppStore(context)
        if (store.medication(medicationId) == null) return

        store.setPendingReminder(
            PendingReminder(
                medicationId = medicationId,
                dueAt = System.currentTimeMillis()
            )
        )

        val serviceIntent = Intent(context, ReminderSoundService::class.java).apply {
            action = ReminderSoundService.ACTION_START
            putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, medicationId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
