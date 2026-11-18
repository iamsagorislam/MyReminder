package com.sagor.myreminder.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sagor.myreminder.AlertActivity
import com.sagor.myreminder.MainActivity
import com.sagor.myreminder.R
import com.sagor.myreminder.data.Reminder

object NotificationHelper {
    const val CH_STANDARD = "standard"
    const val CH_IMPORTANT = "important"
    const val CH_URGENT = "urgent"

    fun createChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_STANDARD, "Standard reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_IMPORTANT, "Important reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
        )
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(
            NotificationChannel(CH_URGENT, "Urgent full-screen alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800, 300, 800)
                setSound(alarmSound, attrs)
            }
        )
    }

    fun canFullScreen(c: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34)
            c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        else true

    private fun action(c: Context, code: Int, id: Long, act: String, minutes: Int = 0): PendingIntent {
        val i = Intent(c, ActionReceiver::class.java).setAction(act)
            .putExtra("id", id).putExtra("min", minutes)
        return PendingIntent.getBroadcast(
            c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun timeText(r: Reminder): String {
        val h = r.minuteOfDay / 60
        val m = r.minuteOfDay % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
    }

    fun show(c: Context, r: Reminder) {
        val nm = c.getSystemService(NotificationManager::class.java)
        val channel = when (r.style) {
            0 -> CH_STANDARD
            1 -> CH_IMPORTANT
            else -> CH_URGENT
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(c, r.id.toInt(), Intent(c, MainActivity::class.java), flags)
        val base = r.id.toInt() * 10

        val text = if (r.notes.isBlank()) timeText(r) else timeText(r) + "  " + r.notes
        val b = NotificationCompat.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(if (r.style == 2) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (r.style == 0) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Complete", action(c, base + 1, r.id, ActionReceiver.COMPLETE))
            .addAction(0, "Snooze 10m", action(c, base + 2, r.id, ActionReceiver.SNOOZE, 10))
            .addAction(0, "Snooze 1h", action(c, base + 3, r.id, ActionReceiver.SNOOZE, 60))

        if (r.style == 2 && canFullScreen(c)) {
            val alert = Intent(c, AlertActivity::class.java).putExtra("id", r.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            b.setFullScreenIntent(PendingIntent.getActivity(c, r.id.toInt(), alert, flags), true)
        }
        nm.notify(r.id.toInt(), b.build())
    }
}
