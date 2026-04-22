package com.hvasoft.dailydose.domain.model

import com.google.common.truth.Truth.assertThat
import java.util.TimeZone
import org.junit.Test

class DailyPromptDayTest {

    @Test
    fun `dateKeyFor uses the local calendar day instead of utc`() {
        withDefaultTimeZone("America/Mexico_City") {
            val localApr20At730Pm = 1_776_733_800_000L

            assertThat(DailyPromptDay.dateKeyFor(localApr20At730Pm)).isEqualTo("2026-04-20")
        }
    }

    @Test
    fun `isSamePromptDay matches a post created earlier on the same local day`() {
        withDefaultTimeZone("America/Mexico_City") {
            val localApr20At730Pm = 1_776_733_800_000L
            val localApr20At1030Pm = 1_776_744_600_000L

            assertThat(
                DailyPromptDay.isSamePromptDay(
                    timestampMillis = localApr20At730Pm,
                    dateKey = DailyPromptDay.dateKeyFor(localApr20At1030Pm),
                ),
            ).isTrue()
        }
    }

    private fun withDefaultTimeZone(
        timeZoneId: String,
        block: () -> Unit,
    ) {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId))
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
