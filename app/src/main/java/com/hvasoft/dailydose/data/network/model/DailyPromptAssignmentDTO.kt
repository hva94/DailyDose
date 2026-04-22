package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DailyPromptAssignmentDTO(
    val comboId: String = "",
    val promptText: String = "",
    val titlePatterns: List<String> = emptyList(),
    val answerFormats: List<String> = emptyList(),
    val assignedAt: Long = 0L,
    val previousComboId: String? = null,
)
