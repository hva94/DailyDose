package com.hvasoft.dailydose.presentation.screens.home

import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import org.junit.Test

class HomeFeedUiStateTest {

    @Test
    fun `shouldShowDailyPromptCard stays false while prompt state is still loading`() {
        val uiState = HomeFeedUiState(
            activeDailyPrompt = dailyPromptAssignment(),
            isPromptLoading = true,
            promptAvailabilityMode = HomePromptAvailabilityMode.AVAILABLE,
        )

        assertThat(uiState.shouldShowDailyPromptCard).isFalse()
    }

    @Test
    fun `shouldShowDailyPromptCard becomes true once prompt state is loaded`() {
        val uiState = HomeFeedUiState(
            activeDailyPrompt = dailyPromptAssignment(),
            isPromptLoading = false,
            promptAvailabilityMode = HomePromptAvailabilityMode.AVAILABLE,
        )

        assertThat(uiState.shouldShowDailyPromptCard).isTrue()
    }

    private fun dailyPromptAssignment() = DailyPromptAssignment(
        dateKey = "2026-04-24",
        comboId = "daily-prompt-1",
        promptText = "What stood out today?",
        titlePatterns = listOf("What stood out at %time"),
        answerFormats = listOf("{answer} at %time"),
        assignedAt = 10L,
    )
}
