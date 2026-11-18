package com.sagor.myreminder.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sagor.myreminder.MainActivity
import com.sagor.myreminder.data.Reminder
import com.sagor.myreminder.notifications.AlarmReceiver
import java.time.ZoneId

object AlarmScheduler {
    private const val SNOOZE_OFFSET = 1_000_000
    private const val NAG_OFFSET = 2_000_000

    fun canExact(c: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 31) c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        else true

    private fun pi(c: Context, code: Int, id: Long, nag: Int): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).putExtra("id", id).putExtra("nag", nag)
        return PendingIntent.getBroadcast(
            c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAt(c: Context, code: Int, id: Long, nag: Int, millis: Long, preferClock: Boolean) {
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, code, id, nag)
        try {
            if (canExact(c)) {
                if (preferClock) {
                    val show = PendingIntent.getActivity(
                        c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), p)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, p)
                }
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, p)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, p)
        }
    }

    /** One alarm per reminder, always the next occurrence only. */
    fun schedule(c: Context, r: Reminder) {
        cancelMain(c, r.id)
        if (r.completed) return
        val next = Recurrence.next(r) ?: return
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAt(c, r.id.toInt(), r.id, 0, millis, preferClock = r.style == 2 || r.priority == 3)
    }

    fun scheduleSnooze(c: Context, r: Reminder, minutes: Int) {
        val t = System.currentTimeMillis() + minutes * 60_000L
        setAt(c, r.id.toInt() + SNOOZE_OFFSET, r.id, 0, t, preferClock = true)
    }

    fun scheduleNag(c: Context, r: Reminder, count: Int) {
        val t = System.currentTimeMillis() + r.nagMinutes * 60_000L
        setAt(c, r.id.toInt() + NAG_OFFSET, r.id, count, t, preferClock = false)
    }

    private fun cancelMain(c: Context, id: Long) = cancelCode(c, id.toInt(), id)

    /** Cancels pending snooze and follow-up alarms, keeps the main schedule. */
    fun cancelExtras(c: Context, id: Long) {
        cancelCode(c, id.toInt() + SNOOZE_OFFSET, id)
        cancelCode(c, id.toInt() + NAG_OFFSET, id)
    }

    fun cancelAll(c: Context, id: Long) {
        cancelMain(c, id)
        cancelExtras(c, id)
    }

    private fun cancelCode(c: Context, code: Int, id: Long) {
        c.getSystemService(AlarmManager::class.java).cancel(pi(c, code, id, 0))
    }
}
