package com.hvasoft.dailydose.presentation.screens.home

import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.domain.common.error_handling.ErrorState

sealed class HomeState {
    object Loading : HomeState()
    object Empty : HomeState()
    data class Success(val snapshots: List<Snapshot>) : HomeState()
    data class Failure(val error: ErrorState? = null) : HomeState()
}