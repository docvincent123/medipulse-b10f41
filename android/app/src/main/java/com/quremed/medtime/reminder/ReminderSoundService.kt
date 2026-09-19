package com.quremed.medtime.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.quremed.medtime.MainActivity
import com.quremed.medtime.R
import com.quremed.medtime.data.AppStore

class ReminderSoundService : Service() {
    private var player: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        val medicationId = intent?.getStringExtra(ReminderScheduler.EXTRA_MEDICATION_ID)
            ?: return START_NOT_STICKY
        val store = AppStore(this)
        val medication = store.medication(medicationId) ?: return START_NOT_STICKY

        startForeground(NOTIFICATION_ID, notification(medicationId, medication.name, medication.dose))
        playSound(store.customSoundUri())
        return START_NOT_STICKY
    }

    private fun notification(medicationId: String, name: String, dose: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Час прийняти $name")
            .setContentText(dose.ifBlank { "Відкрийте MedTime та підтвердьте прийом" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent(medicationId, false))
            .addAction(
                R.drawable.ic_check,
                "Прийняв",
                broadcastIntent(ReminderActionReceiver.ACTION_TAKEN, medicationId, 1001)
            )
            .addAction(
                R.drawable.ic_snooze,
                "Через 10 хв",
                broadcastIntent(ReminderActionReceiver.ACTION_SNOOZE, medicationId, 1002)
            )
            .addAction(
                R.drawable.ic_close,
                "Не прийняв",
                openAppIntent(medicationId, true)
            )
            .build()

    private fun broadcastIntent(action: String, medicationId: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, medicationId)
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode + medicationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppIntent(medicationId: String, askReason: Boolean): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MainActivity.EXTRA_ASK_MISSED_REASON, askReason)
        }
        return PendingIntent.getActivity(
            this,
            medicationId.hashCode() + if (askReason) 5000 else 4000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun playSound(customUri: Uri?) {
        player?.release()
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (customUri != null) {
                    setDataSource(this@ReminderSoundService, customUri)
                    prepare()
                } else {
                    val afd = resources.openRawResourceFd(R.raw.medicine_alarm)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                }
                isLooping = true
                start()
            }
        }.getOrElse {
            MediaPlayer.create(this, R.raw.medicine_alarm)?.apply {
                isLooping = true
                start()
            }
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Нагадування про ліки",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Важливі нагадування MedTime"
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun stopSelfSafely() {
        player?.stop()
        player?.release()
        player = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.quremed.medtime.service.START"
        const val ACTION_STOP = "com.quremed.medtime.service.STOP"
        private const val CHANNEL_ID = "medtime_reminders"
        private const val NOTIFICATION_ID = 9400

        fun stop(context: Context) {
            context.stopService(Intent(context, ReminderSoundService::class.java))
        }
    }
}
