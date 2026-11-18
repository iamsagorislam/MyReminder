package com.sagor.myreminder.scheduling

import com.sagor.myreminder.data.Reminder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Recurrence {
    /** Next trigger strictly after [now], or null if a one-time reminder is already in the past. */
    fun next(r: Reminder, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val base = LocalDate.ofEpochDay(r.epochDay)
        val time = LocalTime.of(r.minuteOfDay / 60, r.minuteOfDay % 60)
        if (r.repeat == 0) {
            val c = LocalDateTime.of(base, time)
            return if (c.isAfter(now)) c else null
        }
        var d = if (now.toLocalDate().isAfter(base)) now.toLocalDate() else base
        repeat(9) {
            if (matches(r.repeat, base, d)) {
                val c = LocalDateTime.of(d, time)
                if (c.isAfter(now)) return c
            }
            d = d.plusDays(1)
        }
        return null
    }

    private fun matches(rule: Int, base: LocalDate, d: LocalDate): Boolean = when (rule) {
        1 -> true
        2 -> d.dayOfWeek == base.dayOfWeek
        3 -> d.dayOfWeek.value <= 5
        4 -> d.dayOfWeek.value >= 6
        else -> false
    }
}
