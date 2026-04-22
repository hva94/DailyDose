package com.hvasoft.dailydose.domain.model

data class DailyPromptCombo(
    val comboId: String,
    val promptText: String,
    val titlePatterns: List<String>,
    val answerFormats: List<String>,
    val isEnabled: Boolean = true,
)
