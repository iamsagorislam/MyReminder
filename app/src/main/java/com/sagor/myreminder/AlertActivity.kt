package com.sagor.myreminder

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagor.myreminder.data.AppDb
import com.sagor.myreminder.data.Reminder
import com.sagor.myreminder.notifications.NotificationHelper
import com.sagor.myreminder.notifications.ReminderActions
import com.sagor.myreminder.ui.MyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val id = intent.getLongExtra("id", -1)
        setContent { MyTheme { AlertScreen(id) { finish() } } }
    }
}

@Composable
private fun AlertScreen(id: Long, close: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reminder by remember { mutableStateOf<Reminder?>(null) }
    var custom by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        reminder = withContext(Dispatchers.IO) { AppDb.get(ctx).dao().get(id) }
    }

    fun run(block: suspend () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) { block() }
            close()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val r = reminder
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (r == null) {
                Text("Reminder not found", fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = close) { Text("Close") }
            } else {
                Text(
                    NotificationHelper.timeText(r),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    r.title,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (r.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(r.notes, fontSize = 18.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(36.dp))
                Button(
                    onClick = { run { ReminderActions.complete(ctx, id) } },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Complete", fontSize = 18.sp) }

                Spacer(Modifier.height(20.dp))
                Text("Snooze")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 60 to "1h").forEach { (m, label) ->
                        OutlinedButton(
                            onClick = { run { ReminderActions.snooze(ctx, id, m) } },
                            modifier = Modifier.weight(1f)
                        ) { Text(label, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { custom = it.filter(Char::isDigit).take(4) },
                        label = { Text("Custom (min)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        enabled = (custom.toIntOrNull() ?: 0) > 0,
                        onClick = { run { ReminderActions.snooze(ctx, id, custom.toInt()) } }
                    ) { Text("Snooze") }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { ReminderActions.dismiss(ctx, id); close() }) { Text("Dismiss") }
            }
        }
    }
}
