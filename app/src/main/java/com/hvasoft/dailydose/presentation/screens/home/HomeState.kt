package com.hvasoft.dailydose.presentation.screens.home

import com.hvasoft.dailydose.data.network.model.Snapshot

sealed class HomeState {
    object Loading : HomeState()
    object Empty : HomeState()
    data class Success(val snapshots: List<Snapshot>) : HomeState()
    data class Failure(val errorMessage: String? = null) : HomeState()
}