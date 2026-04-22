package com.hvasoft.dailydose.presentation.screens.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hvasoft.dailydose.domain.interactor.add.CreateSnapshotUseCase
import com.hvasoft.dailydose.domain.model.CreateSnapshotRequest
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.PromptComposerState
import com.hvasoft.dailydose.domain.model.SnapshotTitleGenerationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(
    private val createSnapshotUseCase: CreateSnapshotUseCase,
    private val promptTitleGenerator: PromptTitleGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddPostUiState>(AddPostUiState.Idle)
    val uiState = _uiState.asStateFlow()
    private val _composerState = MutableStateFlow(PromptComposerState())
    val composerState = _composerState.asStateFlow()

    fun prepareStandardComposer() {
        _composerState.value = PromptComposerState()
    }

    fun preparePromptComposer(prompt: DailyPromptAssignment) {
        _composerState.value = promptTitleGenerator.createPromptComposerState(prompt)
    }

    fun onAnswerChanged(answerText: String) {
        _composerState.value = promptTitleGenerator.updateAnswer(
            state = _composerState.value,
            answerText = answerText,
        )
    }

    fun onTitleChanged(title: String) {
        val currentState = _composerState.value
        _composerState.value = currentState.copy(
            draftTitle = title,
            isTitleUserEdited = true,
            titleGenerationMode = if (currentState.activePrompt != null) {
                SnapshotTitleGenerationMode.MANUAL_EDIT
            } else {
                SnapshotTitleGenerationMode.NONE
            },
        )
    }

    fun onImageSelected(
        imageUri: Uri,
        standardDraftTitle: String,
    ) {
        val currentState = _composerState.value
        val nextTitle = when {
            currentState.activePrompt != null -> currentState.draftTitle
            currentState.isTitleUserEdited -> currentState.draftTitle
            else -> standardDraftTitle
        }
        _composerState.value = currentState.copy(
            selectedImageUri = imageUri.toString(),
            draftTitle = nextTitle,
        )
    }

    fun postSnapshot() {
        val currentState = _composerState.value
        val imageUri = currentState.selectedImageUri ?: return
        val publishedAt = System.currentTimeMillis()
        val resolvedTitle = when {
            currentState.activePrompt == null -> currentState.draftTitle.trim()
            currentState.isTitleUserEdited -> currentState.draftTitle.trim()
            else -> promptTitleGenerator.regenerateManagedTitle(
                state = currentState,
                timestampMillis = publishedAt,
            )
        }
        val request = CreateSnapshotRequest(
            title = resolvedTitle,
            localImageContentUri = imageUri,
            dailyPromptId = currentState.activePrompt?.comboId,
            dailyPromptText = currentState.activePrompt?.promptText,
            titleGenerationMode = when {
                currentState.activePrompt == null -> SnapshotTitleGenerationMode.NONE
                currentState.isTitleUserEdited -> SnapshotTitleGenerationMode.MANUAL_EDIT
                currentState.answerText.isBlank() -> SnapshotTitleGenerationMode.PROMPT_PATTERN
                else -> SnapshotTitleGenerationMode.ANSWER_FORMAT
            },
        )

        viewModelScope.launch {
            _uiState.value = AddPostUiState.Uploading(0)
            val outcome = createSnapshotUseCase(
                request = request,
                onProgress = { percent -> _uiState.value = AddPostUiState.Uploading(percent) },
            )
            _uiState.value = when (outcome) {
                is CreateSnapshotResult.Success -> {
                    _composerState.value = PromptComposerState()
                    AddPostUiState.Success(outcome.snapshot)
                }
                CreateSnapshotResult.ImageUploadFailed -> AddPostUiState.FailedImage
                CreateSnapshotResult.SaveFailed -> AddPostUiState.FailedSave
            }
        }
    }

    fun acknowledgeTerminalState() {
        when (_uiState.value) {
            is AddPostUiState.Success,
            AddPostUiState.FailedImage,
            AddPostUiState.FailedSave,
            -> _uiState.value = AddPostUiState.Idle
            else -> Unit
        }
    }
}
