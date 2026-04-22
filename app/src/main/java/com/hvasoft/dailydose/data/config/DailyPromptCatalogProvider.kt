package com.hvasoft.dailydose.data.config

import com.hvasoft.dailydose.domain.model.DailyPromptCombo

interface DailyPromptCatalogProvider {
    fun getDailyPromptCombos(): List<DailyPromptCombo>
}
