package com.hvasoft.dailydose.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedOfflineTypeConvertersTest {

    private val converters = FeedOfflineTypeConverters()

    @Test
    fun `fromReactionSummary stores empty maps as a non-null value`() {
        val encoded = converters.fromReactionSummary(emptyMap())

        assertThat(encoded).isNotNull()
        assertThat(encoded).isEmpty()
    }

    @Test
    fun `reaction summary round trips emoji counts`() {
        val encoded = converters.fromReactionSummary(
            mapOf(
                "\u2764\uFE0F" to 2,
                "\uD83D\uDD25" to 1,
            ),
        )

        val decoded = converters.toReactionSummary(encoded)

        assertThat(decoded).isEqualTo(
            mapOf(
                "\u2764\uFE0F" to 2,
                "\uD83D\uDD25" to 1,
            ),
        )
    }
}
