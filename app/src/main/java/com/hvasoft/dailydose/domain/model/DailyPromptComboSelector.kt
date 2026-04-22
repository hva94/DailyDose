package com.hvasoft.dailydose.domain.model

import java.lang.Math.floorMod

object DailyPromptComboSelector {

    fun resolveForDay(
        combos: List<DailyPromptCombo>,
        dateKey: String,
        previousComboId: String?,
    ): DailyPromptCombo? {
        val enabledCombos = combos.filter { combo ->
            combo.isEnabled &&
                combo.promptText.isNotBlank() &&
                combo.titlePatterns.isNotEmpty() &&
                combo.answerFormats.isNotEmpty()
        }
        if (enabledCombos.isEmpty()) return null

        val baseIndex = floorMod(dateKey.hashCode(), enabledCombos.size)
        return enabledCombos.indices
            .map { offset -> enabledCombos[(baseIndex + offset) % enabledCombos.size] }
            .firstOrNull { it.comboId != previousComboId }
            ?: enabledCombos.first()
    }
}
