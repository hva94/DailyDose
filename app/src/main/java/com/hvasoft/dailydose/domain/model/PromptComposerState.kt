package com.hvasoft.dailydose.domain.model

data class PromptComposerState(
    val activePrompt: DailyPromptAssignment? = null,
    val answerText: String = "",
    val draftTitle: String = "",
    val isTitleUserEdited: Boolean = false,
    val selectedImageUri: String? = null,
    val selectedPromptTitlePattern: String? = null,
    val selectedAnswerFormat: String? = null,
    val titleGenerationMode: SnapshotTitleGenerationMode = SnapshotTitleGenerationMode.NONE,
)
