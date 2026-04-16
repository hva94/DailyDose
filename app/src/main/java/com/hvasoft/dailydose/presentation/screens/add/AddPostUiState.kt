package com.hvasoft.dailydose.presentation.screens.add

import com.hvasoft.dailydose.domain.model.Snapshot

sealed interface AddPostUiState {
    data object Idle : AddPostUiState
    data class Uploading(val percent: Int) : AddPostUiState
    data class Success(val snapshot: Snapshot) : AddPostUiState
    data object FailedImage : AddPostUiState
    data object FailedSave : AddPostUiState
}
