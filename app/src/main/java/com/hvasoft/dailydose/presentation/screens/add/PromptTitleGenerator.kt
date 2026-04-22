package com.hvasoft.dailydose.presentation.screens.add

import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.PromptComposerState
import com.hvasoft.dailydose.domain.model.SnapshotTitleGenerationMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

class PromptTitleGenerator @Inject constructor() {

    fun createPromptComposerState(
        prompt: DailyPromptAssignment,
        timestampMillis: Long = System.currentTimeMillis(),
    ): PromptComposerState {
        val selectionSeed = prompt.dateKey.hashCode().toLong() xor timestampMillis
        val selectedPromptPattern = prompt.titlePatterns.pick(selectionSeed)
            ?: return PromptComposerState()
        val selectedAnswerFormat = prompt.answerFormats.pick(selectionSeed + 1L)
            ?: return PromptComposerState()

        return PromptComposerState(
            activePrompt = prompt,
            draftTitle = applyPromptPattern(selectedPromptPattern, timestampMillis),
            selectedPromptTitlePattern = selectedPromptPattern,
            selectedAnswerFormat = selectedAnswerFormat,
            titleGenerationMode = SnapshotTitleGenerationMode.PROMPT_PATTERN,
        )
    }

    fun updateAnswer(
        state: PromptComposerState,
        answerText: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ): PromptComposerState {
        val trimmedAnswer = answerText.trim()
        if (state.activePrompt == null) {
            return state.copy(answerText = answerText)
        }
        if (state.isTitleUserEdited) {
            return state.copy(answerText = answerText)
        }

        val selectedPromptPattern = state.selectedPromptTitlePattern ?: return state.copy(answerText = answerText)
        val selectedAnswerFormat = state.selectedAnswerFormat ?: return state.copy(answerText = answerText)
        val titleGenerationMode = if (trimmedAnswer.isBlank()) {
            SnapshotTitleGenerationMode.PROMPT_PATTERN
        } else {
            SnapshotTitleGenerationMode.ANSWER_FORMAT
        }
        val draftTitle = when (titleGenerationMode) {
            SnapshotTitleGenerationMode.ANSWER_FORMAT -> applyAnswerFormat(
                format = selectedAnswerFormat,
                answer = trimmedAnswer,
                timestampMillis = timestampMillis,
            )

            else -> applyPromptPattern(selectedPromptPattern, timestampMillis)
        }

        return state.copy(
            answerText = answerText,
            draftTitle = draftTitle,
            selectedPromptTitlePattern = selectedPromptPattern,
            selectedAnswerFormat = selectedAnswerFormat,
            titleGenerationMode = titleGenerationMode,
        )
    }

    fun regenerateManagedTitle(
        state: PromptComposerState,
        timestampMillis: Long = System.currentTimeMillis(),
    ): String {
        val selectedPromptPattern = state.selectedPromptTitlePattern ?: return state.draftTitle.trim()
        val selectedAnswerFormat = state.selectedAnswerFormat ?: return state.draftTitle.trim()
        val trimmedAnswer = state.answerText.trim()

        return if (trimmedAnswer.isBlank()) {
            applyPromptPattern(selectedPromptPattern, timestampMillis)
        } else {
            applyAnswerFormat(
                format = selectedAnswerFormat,
                answer = trimmedAnswer,
                timestampMillis = timestampMillis,
            )
        }
    }

    private fun applyPromptPattern(
        pattern: String,
        timestampMillis: Long,
    ): String = pattern.replace(TIME_PLACEHOLDER, formatTime(timestampMillis))

    private fun applyAnswerFormat(
        format: String,
        answer: String,
        timestampMillis: Long,
    ): String = format
        .replace(ANSWER_PLACEHOLDER, answer)
        .replace(TIME_PLACEHOLDER, formatTime(timestampMillis))

    private fun formatTime(timestampMillis: Long): String =
        SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(Date(timestampMillis))

    private fun List<String>.pick(seed: Long): String? =
        if (isEmpty()) null else this[Random(seed).nextInt(size)]

    private companion object {
        const val TIME_PLACEHOLDER = "%time"
        const val ANSWER_PLACEHOLDER = "{answer}"
        const val TIME_PATTERN = "h:mm a"
    }
}
