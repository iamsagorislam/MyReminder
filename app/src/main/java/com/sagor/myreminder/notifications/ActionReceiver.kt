package com.sagor.myreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getLongExtra("id", -1)
        val minutes = i.getIntExtra("min", 10)
        val action = i.action
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    COMPLETE -> ReminderActions.complete(c, id)
                    SNOOZE -> ReminderActions.snooze(c, id, minutes)
                    else -> ReminderActions.dismiss(c, id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val COMPLETE = "com.sagor.myreminder.COMPLETE"
        const val SNOOZE = "com.sagor.myreminder.SNOOZE"
    }
}
