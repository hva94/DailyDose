package com.hvasoft.dailydose.presentation.screens.add

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.MainDispatcherRule
import com.hvasoft.dailydose.domain.interactor.add.CreateSnapshotUseCase
import com.hvasoft.dailydose.domain.model.PostSnapshotOutcome
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
                    title: String,
                    localImageContentUri: String,
                    onProgress: (Int) -> Unit,
                ): PostSnapshotOutcome = PostSnapshotOutcome.SAVE_FAILED
            },
        )

        viewModel.postSnapshot(
            title = "A post",
            imageUri = imageUri,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(AddPostUiState.FailedSave)
    }
}
