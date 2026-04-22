package com.hvasoft.dailydose.domain.model

data class CreateSnapshotRequest(
    val title: String,
    val localImageContentUri: String,
    val dailyPromptId: String? = null,
    val dailyPromptText: String? = null,
    val titleGenerationMode: SnapshotTitleGenerationMode = SnapshotTitleGenerationMode.NONE,
)
