package ru.vladis.realtorai.local

import java.time.LocalDateTime
import java.time.ZoneId

object MeetingPlan {
    private val appointment = Regex(
        """(?:показ|встреча)\s*[:—-]?\s*(\d{1,2})[.](\d{1,2})[.](\d{4})\s+(?:в\s+)?(\d{1,2}):(\d{2})""",
        RegexOption.IGNORE_CASE
    )

    fun parse(note: String, now: Long, zone: ZoneId): Long? {
        val match = appointment.matchEntire(note.trim()) ?: return null
        val (day, month, year, hour, minute) = match.destructured
        val date = runCatching {
            LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
        }.getOrNull() ?: return null
        val offset = zone.rules.getValidOffsets(date).singleOrNull() ?: return null
        return date.toInstant(offset).toEpochMilli().takeIf { it > now }
    }
}
