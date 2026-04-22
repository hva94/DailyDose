package com.hvasoft.dailydose.presentation.screens.add

import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.SnapshotTitleGenerationMode
import org.junit.Test

class PromptTitleGeneratorTest {

    private val generator = PromptTitleGenerator()
    private val prompt = DailyPromptAssignment(
        dateKey = "2026-04-20",
        comboId = "daily-prompt-2",
        promptText = "What stood out today?",
        titlePatterns = listOf(
            "This stood out at %time",
            "Something stood out at %time",
        ),
        answerFormats = listOf(
            "{answer} · %time",
            "{answer} at %time",
        ),
        assignedAt = 10L,
    )

    @Test
    fun `createPromptComposerState uses a prompt pattern when no answer exists`() {
        val state = generator.createPromptComposerState(
            prompt = prompt,
            timestampMillis = 1_713_571_200_000L,
        )

        assertThat(state.activePrompt).isEqualTo(prompt)
        assertThat(state.answerText).isEmpty()
        assertThat(state.selectedPromptTitlePattern).isNotNull()
        assertThat(state.draftTitle).contains("at")
        assertThat(state.titleGenerationMode).isEqualTo(SnapshotTitleGenerationMode.PROMPT_PATTERN)
    }

    @Test
    fun `updateAnswer switches the managed draft to an answer based title`() {
        val startingState = generator.createPromptComposerState(
            prompt = prompt,
            timestampMillis = 1_713_571_200_000L,
        )

        val answerState = generator.updateAnswer(
            state = startingState,
            answerText = "Live music and friends",
            timestampMillis = 1_713_571_200_000L,
        )

        assertThat(answerState.draftTitle).contains("Live music and friends")
        assertThat(answerState.titleGenerationMode).isEqualTo(SnapshotTitleGenerationMode.ANSWER_FORMAT)
    }

    @Test
    fun `regenerateManagedTitle refreshes the timestamp while preserving the answer`() {
        val startingState = generator.createPromptComposerState(
            prompt = prompt,
            timestampMillis = 1_713_571_200_000L,
        )
        val answerState = generator.updateAnswer(
            state = startingState,
            answerText = "Cafe stop",
            timestampMillis = 1_713_571_200_000L,
        )

        val regeneratedTitle = generator.regenerateManagedTitle(
            state = answerState,
            timestampMillis = 1_713_574_800_000L,
        )

        assertThat(regeneratedTitle).contains("Cafe stop")
        assertThat(regeneratedTitle).isNotEqualTo(answerState.draftTitle)
    }

    @Test
    fun `createPromptComposerState fails closed when assignment templates are missing`() {
        val state = generator.createPromptComposerState(
            prompt = prompt.copy(
                titlePatterns = emptyList(),
                answerFormats = emptyList(),
            ),
            timestampMillis = 1_713_571_200_000L,
        )

        assertThat(state.activePrompt).isNull()
        assertThat(state.draftTitle).isEmpty()
        assertThat(state.selectedPromptTitlePattern).isNull()
        assertThat(state.selectedAnswerFormat).isNull()
    }
}
