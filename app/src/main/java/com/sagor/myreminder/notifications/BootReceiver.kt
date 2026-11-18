package com.sagor.myreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sagor.myreminder.data.AppDb
import com.sagor.myreminder.scheduling.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reschedules everything after reboot, app update, clock change or timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDb.get(c).dao().active().forEach { AlarmScheduler.schedule(c, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
