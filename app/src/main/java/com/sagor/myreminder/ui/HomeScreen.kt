package com.sagor.myreminder.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.sagor.myreminder.data.AppDb
import com.sagor.myreminder.data.Reminder
import com.sagor.myreminder.notifications.NotificationHelper
import com.sagor.myreminder.notifications.ReminderActions
import com.sagor.myreminder.scheduling.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val repeatLabels = listOf("Once", "Daily", "Weekly", "Weekdays", "Weekends")
private val priorityLabels = listOf("Low", "Medium", "High", "Urgent")
private val styleLabels = listOf("Standard", "Important", "Full-screen")
private val priorityColors = listOf(Color(0xFF8A94A6), Color(0xFF1E6BFF), Color(0xFFF59E0B), Color(0xFFE5393B))

@Composable
fun HomeScreen(tick: Int) {
    val ctx = LocalContext.current
    val dao = remember { AppDb.get(ctx).dao() }
    val items by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val notifOk = remember(tick) { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val exactOk = remember(tick) { AlarmScheduler.canExact(ctx) }
    val fullOk = remember(tick) { NotificationHelper.canFullScreen(ctx) }

    val now = LocalDateTime.now()
    fun dt(r: Reminder) = LocalDateTime.of(
        LocalDate.ofEpochDay(r.epochDay), LocalTime.of(r.minuteOfDay / 60, r.minuteOfDay % 60)
    )
    val done = items.filter { it.completed }
    val overdue = items.filter { !it.completed && it.repeat == 0 && dt(it).isBefore(now) }
    val upcoming = items.filter { !it.completed && it !in overdue }

    fun toggleDone(r: Reminder) {
        scope.launch(Dispatchers.IO) {
            if (r.completed) {
                val back = r.copy(completed = false)
                dao.update(back)
                AlarmScheduler.schedule(ctx, back)
            } else {
                ReminderActions.complete(ctx, r.id)
            }
        }
    }

    fun delete(r: Reminder) {
        scope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelAll(ctx, r.id)
            dao.delete(r)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New reminder")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("My Reminder", color = MaterialTheme.colorScheme.primary)
            }
            if (!notifOk) item {
                WarnCard("Notifications are blocked. Reminders cannot appear.", "Allow") {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            if (!exactOk) item {
                WarnCard("Exact alarms are off. Reminders may arrive late.", "Allow") {
                    if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            if (!fullOk) item {
                WarnCard("Full-screen alerts are off. Urgent reminders will show as a normal pop-up.", "Allow") {
                    if (Build.VERSION.SDK_INT >= 34) ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + ctx.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            if (items.isEmpty()) item {
                Text(
                    "Nothing yet. Tap + to add your first reminder.",
                    modifier = Modifier.padding(top = 32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            section("Overdue", overdue, ::dt, { toggleDone(it) }, { delete(it) }) { editing = it; showDialog = true }
            section("Upcoming", upcoming, ::dt, { toggleDone(it) }, { delete(it) }) { editing = it; showDialog = true }
            section("Completed", done, ::dt, { toggleDone(it) }, { delete(it) }) { editing = it; showDialog = true }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showDialog) {
        ReminderDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { r ->
                showDialog = false
                scope.launch(Dispatchers.IO) {
                    val id = if (r.id == 0L) dao.insert(r) else { dao.update(r); r.id }
                    AlarmScheduler.schedule(ctx, r.copy(id = id))
                }
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    list: List<Reminder>,
    dt: (Reminder) -> LocalDateTime,
    onToggle: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit
) {
    if (list.isEmpty()) return
    item(key = "h_$title") {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
    items(list, key = { it.id }) { r -> ReminderCard(r, dt(r), onToggle, onDelete, onEdit) }
}

@Composable
private fun WarnCard(text: String, button: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E0))
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, Modifier.weight(1f), color = Color(0xFF5B3B00), fontSize = 14.sp)
            TextButton(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
private fun ReminderCard(
    r: Reminder,
    time: LocalDateTime,
    onToggle: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit
) {
    val today = LocalDate.now()
    val dayText = when (time.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> time.toLocalDate().format(DateTimeFormatter.ofPattern("d MMM"))
    }
    val repeatText = if (r.repeat == 0) "" else "  \u2022  " + repeatLabels[r.repeat]
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable { onEdit(r) }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onToggle(r) }) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(if (r.completed) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (r.completed) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    else Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(priorityColors[r.priority].copy(alpha = 0.25f))
                    )
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(
                    r.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textDecoration = if (r.completed) TextDecoration.LineThrough else null
                )
                Text(
                    dayText + ", " + NotificationHelper.timeText(r) + repeatText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(Modifier.size(10.dp).clip(CircleShape).background(priorityColors[r.priority]))
            IconButton(onClick = { onDelete(r) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun ReminderDialog(initial: Reminder?, onDismiss: () -> Unit, onSave: (Reminder) -> Unit) {
    val ctx: Context = LocalContext.current
    val start = remember { LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var date by remember {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.epochDay) } ?: start.toLocalDate())
    }
    var minute by remember { mutableIntStateOf(initial?.minuteOfDay ?: (start.hour * 60)) }
    var repeat by remember { mutableIntStateOf(initial?.repeat ?: 0) }
    var priority by remember { mutableIntStateOf(initial?.priority ?: 1) }
    var style by remember { mutableIntStateOf(initial?.style ?: 1) }
    var nag by remember { mutableStateOf(initial?.nagEnabled ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New reminder" else "Edit reminder") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("What do you need to do?") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        DatePickerDialog(
                            ctx, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year, date.monthValue - 1, date.dayOfMonth
                        ).show()
                    }) { Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))) }
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            ctx, { _, h, m -> minute = h * 60 + m }, minute / 60, minute % 60, false
                        ).show()
                    }) {
                        Text(NotificationHelper.timeText(Reminder(title = "", epochDay = 0, minuteOfDay = minute)))
                    }
                }
                ChipRow("Repeat", repeatLabels, repeat) { repeat = it }
                ChipRow("Priority", priorityLabels, priority) { priority = it }
                ChipRow("Alert style", styleLabels, style) { style = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remind again every 15 min if not done (max 3 times)", Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = nag, onCheckedChange = { nag = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        Reminder(
                            id = initial?.id ?: 0,
                            title = title.trim(),
                            notes = notes.trim(),
                            epochDay = date.toEpochDay(),
                            minuteOfDay = minute,
                            repeat = repeat,
                            priority = priority,
                            style = style,
                            completed = false,
                            nagEnabled = nag,
                            nagMinutes = 15
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChipRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { i, text ->
                FilterChip(selected = selected == i, onClick = { onSelect(i) }, label = { Text(text) })
            }
        }
    }
}
