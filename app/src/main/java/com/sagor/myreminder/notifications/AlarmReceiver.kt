package com.sagor.myreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sagor.myreminder.data.AppDb
import com.sagor.myreminder.scheduling.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getLongExtra("id", -1)
        val nag = i.getIntExtra("nag", 0)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = AppDb.get(c).dao().get(id)
                if (r != null && !r.completed) {
                    NotificationHelper.show(c, r)
                    if (r.repeat != 0) AlarmScheduler.schedule(c, r)
                    if (r.nagEnabled && nag < MAX_NAG) AlarmScheduler.scheduleNag(c, r, nag + 1)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val MAX_NAG = 3
    }
}
