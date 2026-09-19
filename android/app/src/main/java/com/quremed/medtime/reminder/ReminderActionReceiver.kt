package com.quremed.medtime.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quremed.medtime.data.AppStore

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICATION_ID) ?: return
        val store = AppStore(context)
        val medication = store.medication(medicationId) ?: return
        val pending = store.pendingReminder()
        val dueAt = pending?.dueAt ?: System.currentTimeMillis()

        when (intent.action) {
            ACTION_TAKEN -> {
                store.markTaken(medicationId, dueAt)
                ReminderScheduler.scheduleNextDay(context, medication)
            }
            ACTION_SNOOZE -> {
                store.markSnoozed(medicationId, dueAt)
                ReminderScheduler.scheduleAt(
                    context,
                    medicationId,
                    System.currentTimeMillis() + TEN_MINUTES
                )
            }
        }
        CloudSyncWorker.enqueue(context)
        ReminderSoundService.stop(context)
    }

    companion object {
        const val ACTION_TAKEN = "com.quremed.medtime.action.TAKEN"
        const val ACTION_SNOOZE = "com.quremed.medtime.action.SNOOZE"
        private const val TEN_MINUTES = 10 * 60 * 1000L
    }
}
