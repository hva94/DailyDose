package com.hvasoft.dailydose.presentation.screens.home

import androidx.paging.PagingData
import com.hvasoft.dailydose.domain.model.Snapshot

sealed class HomeState {
    object Loading : HomeState()
    object Empty : HomeState()
    data class Success(val pagingData: PagingData<Snapshot>) : HomeState()
    data class Failure(val errorMessage: String? = null) : HomeState()
}