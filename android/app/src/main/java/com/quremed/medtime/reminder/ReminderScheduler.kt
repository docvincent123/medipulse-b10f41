package com.quremed.medtime.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quremed.medtime.data.AppStore
import com.quremed.medtime.data.Medication
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    const val EXTRA_MEDICATION_ID = "medication_id"

    fun scheduleDaily(context: Context, medication: Medication) {
        if (!medication.enabled) return
        scheduleAt(context, medication.id, nextOccurrence(medication.time))
    }

    fun scheduleNextDay(context: Context, medication: Medication) {
        if (!medication.enabled) return
        val next = nextOccurrence(medication.time, forceTomorrow = true)
        scheduleAt(context, medication.id, next)
    }

    fun scheduleAt(context: Context, medicationId: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = reminderPendingIntent(context, medicationId)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, medicationId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(reminderPendingIntent(context, medicationId))
    }

    fun rescheduleAll(context: Context) {
        AppStore(context).medications().filter { it.enabled }.forEach {
            scheduleDaily(context, it)
        }
    }

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun reminderPendingIntent(context: Context, medicationId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        return PendingIntent.getBroadcast(
            context,
            medicationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextOccurrence(time: String, forceTomorrow: Boolean = false): Long {
        val parsed = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(9, 0))
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var dateTime = now.toLocalDate().atTime(parsed)
        if (forceTomorrow || !dateTime.isAfter(now)) dateTime = dateTime.plusDays(1)
        return dateTime.atZone(zone).toInstant().toEpochMilli()
    }
}
