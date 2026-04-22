package com.hvasoft.dailydose.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DailyPromptDay {
    fun currentDateKey(nowMillis: Long = System.currentTimeMillis()): String =
        dateKeyFor(nowMillis)

    fun dateKeyFor(timestampMillis: Long): String =
        Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

    fun previousDateKey(dateKey: String): String =
        LocalDate.parse(dateKey)
            .minusDays(1)
            .toString()

    fun isSamePromptDay(
        timestampMillis: Long?,
        dateKey: String = currentDateKey(),
    ): Boolean = timestampMillis?.let(::dateKeyFor) == dateKey
}
