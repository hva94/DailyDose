package com.hvasoft.dailydose.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DailyPromptComboSelectorTest {

    private val combos = listOf(
        DailyPromptCombo(
            comboId = "daily-prompt-1",
            promptText = "What made today different?",
            titlePatterns = listOf("Today felt different at %time", "A different moment at %time"),
            answerFormats = listOf("{answer} · %time", "{answer} at %time"),
        ),
        DailyPromptCombo(
            comboId = "daily-prompt-2",
            promptText = "What stood out today?",
            titlePatterns = listOf("This stood out at %time", "Something stood out at %time"),
            answerFormats = listOf("{answer} · %time", "{answer} at %time"),
        ),
        DailyPromptCombo(
            comboId = "daily-prompt-3",
            promptText = "What changed today?",
            titlePatterns = listOf("Something changed at %time", "A change at %time"),
            answerFormats = listOf("{answer} · %time", "{answer} at %time"),
        ),
    )

    @Test
    fun `resolveForDay returns the same combo for the same inputs`() {
        val firstSelection = DailyPromptComboSelector.resolveForDay(
            combos = combos,
            dateKey = "2026-04-20",
            previousComboId = "daily-prompt-1",
        )
        val secondSelection = DailyPromptComboSelector.resolveForDay(
            combos = combos,
            dateKey = "2026-04-20",
            previousComboId = "daily-prompt-1",
        )

        assertThat(secondSelection).isEqualTo(firstSelection)
    }

    @Test
    fun `resolveForDay avoids repeating the previous combo when alternatives exist`() {
        val previousSelection = DailyPromptComboSelector.resolveForDay(
            combos = combos,
            dateKey = "2026-04-20",
            previousComboId = null,
        )!!
        val nextSelection = DailyPromptComboSelector.resolveForDay(
            combos = combos,
            dateKey = "2026-04-21",
            previousComboId = previousSelection.comboId,
        )!!

        assertThat(nextSelection.comboId).isNotEqualTo(previousSelection.comboId)
    }
}
