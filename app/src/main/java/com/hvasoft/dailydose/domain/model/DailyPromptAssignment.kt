package com.hvasoft.dailydose.domain.model

data class DailyPromptAssignment(
    val dateKey: String,
    val comboId: String,
    val promptText: String,
    val titlePatterns: List<String>,
    val answerFormats: List<String>,
    val assignedAt: Long,
    val previousComboId: String? = null,
)
