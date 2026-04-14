package com.hvasoft.dailydose.presentation.screens.add

sealed interface AddPostUiState {
    data object Idle : AddPostUiState
    data class Uploading(val percent: Int) : AddPostUiState
    data object Success : AddPostUiState
    data object FailedImage : AddPostUiState
    data object FailedSave : AddPostUiState
}
