package com.sagor.myreminder.notifications

import android.app.NotificationManager
import android.content.Context
import com.sagor.myreminder.data.AppDb
import com.sagor.myreminder.scheduling.AlarmScheduler

/** Shared by the notification buttons and the full-screen alert. */
object ReminderActions {
    private fun clear(c: Context, id: Long) {
        AlarmScheduler.cancelExtras(c, id)
        c.getSystemService(NotificationManager::class.java).cancel(id.toInt())
    }

    suspend fun complete(c: Context, id: Long) {
        val dao = AppDb.get(c).dao()
        val r = dao.get(id) ?: return
        clear(c, id)
        if (r.repeat == 0) dao.update(r.copy(completed = true))
    }

    suspend fun snooze(c: Context, id: Long, minutes: Int) {
        val r = AppDb.get(c).dao().get(id) ?: return
        clear(c, id)
        AlarmScheduler.scheduleSnooze(c, r, minutes)
    }

    fun dismiss(c: Context, id: Long) = clear(c, id)
}
