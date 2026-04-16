package com.hvasoft.dailydose.presentation.screens.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hvasoft.dailydose.domain.interactor.add.CreateSnapshotUseCase
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(
    private val createSnapshotUseCase: CreateSnapshotUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddPostUiState>(AddPostUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun postSnapshot(title: String, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = AddPostUiState.Uploading(0)
            val outcome = createSnapshotUseCase(
                title = title,
                localImageContentUri = imageUri.toString(),
                onProgress = { percent -> _uiState.value = AddPostUiState.Uploading(percent) },
            )
            _uiState.value = when (outcome) {
                is CreateSnapshotResult.Success -> AddPostUiState.Success(outcome.snapshot)
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
