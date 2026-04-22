package com.hvasoft.dailydose.presentation.screens.add

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.domain.interactor.add.CreateSnapshotUseCase
import com.hvasoft.dailydose.domain.model.CreateSnapshotRequest
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.SnapshotTitleGenerationMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `postSnapshot emits failed save when upstream cannot resolve the current user`() = runTest {
        val imageUri = mockk<Uri>()
        every { imageUri.toString() } returns "content://images/test"
        val viewModel = AddViewModel(
            createSnapshotUseCase = object : CreateSnapshotUseCase {
                override suspend fun invoke(
                    request: CreateSnapshotRequest,
                    onProgress: (Int) -> Unit,
                ): CreateSnapshotResult = CreateSnapshotResult.SaveFailed
            },
            promptTitleGenerator = PromptTitleGenerator(),
        )

        viewModel.onImageSelected(imageUri, "A post")
        viewModel.postSnapshot()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(AddPostUiState.FailedSave)
    }

    @Test
    fun `postSnapshot includes prompt metadata when the composer is prompt driven`() = runTest {
        val imageUri = mockk<Uri>()
        every { imageUri.toString() } returns "content://images/test"
        lateinit var capturedRequest: CreateSnapshotRequest
        val viewModel = AddViewModel(
            createSnapshotUseCase = object : CreateSnapshotUseCase {
                override suspend fun invoke(
                    request: CreateSnapshotRequest,
                    onProgress: (Int) -> Unit,
                ): CreateSnapshotResult {
                    capturedRequest = request
                    return CreateSnapshotResult.SaveFailed
                }
            },
            promptTitleGenerator = PromptTitleGenerator(),
        )

        viewModel.preparePromptComposer(
            DailyPromptAssignment(
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
            ),
        )
        viewModel.onAnswerChanged("Sunset")
        viewModel.onImageSelected(imageUri, "Unused")
        viewModel.postSnapshot()
        advanceUntilIdle()

        assertThat(capturedRequest.dailyPromptId).isEqualTo("daily-prompt-2")
        assertThat(capturedRequest.dailyPromptText).isEqualTo("What stood out today?")
        assertThat(capturedRequest.titleGenerationMode).isEqualTo(SnapshotTitleGenerationMode.ANSWER_FORMAT)
        assertThat(capturedRequest.title).contains("Sunset")
    }
}
