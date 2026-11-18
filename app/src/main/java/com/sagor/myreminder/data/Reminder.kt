package com.sagor.myreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * repeat: 0 once, 1 daily, 2 weekly, 3 weekdays, 4 weekends
 * priority: 0 low, 1 medium, 2 high, 3 urgent
 * style: 0 standard, 1 important, 2 full-screen
 * Date and time are stored as local wall-clock values so a timezone change
 * only needs a reschedule, not a data migration.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val epochDay: Long,
    val minuteOfDay: Int,
    val repeat: Int = 0,
    val priority: Int = 1,
    val style: Int = 1,
    val completed: Boolean = false,
    val nagEnabled: Boolean = false,
    val nagMinutes: Int = 15
)
