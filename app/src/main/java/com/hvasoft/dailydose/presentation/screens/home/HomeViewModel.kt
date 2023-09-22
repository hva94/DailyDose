package com.hvasoft.dailydose.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.cachedIn
import androidx.paging.map
import com.hvasoft.dailydose.di.DispatcherIO
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCase
import com.hvasoft.dailydose.domain.model.Snapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @DispatcherIO private val dispatcherIO: CoroutineDispatcher,
    private val getSnapshotsUseCase: GetSnapshotsUseCase,
    private val toggleUserLikeUseCase: ToggleUserLikeUseCase,
    private val deleteSnapshotUseCase: DeleteSnapshotUseCase
) : ViewModel() {

    private var _snapshotsState = MutableStateFlow<HomeState>(HomeState.Loading)
    val snapshotsState = _snapshotsState.asStateFlow()

    init {
        fetchSnapshots()
    }

    @OptIn(ExperimentalPagingApi::class)
    fun fetchSnapshots() {
        _snapshotsState.value = HomeState.Loading
        viewModelScope.launch(dispatcherIO) {
            try {
                getSnapshotsUseCase.invoke()
                    .cachedIn(viewModelScope)
                    .flowOn(dispatcherIO)
                    .map { page -> page.map { snapshot -> snapshot } }
                    .collect { pagingData ->
                        _snapshotsState.value = HomeState.Success(pagingData = pagingData)
                    }
            } catch (e: Exception) {
                _snapshotsState.value = HomeState.Failure(e.localizedMessage)
            }
        }
    }

    fun setLikeSnapshot(snapshot: Snapshot, isChecked: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            toggleUserLikeUseCase.invoke(snapshot, isChecked)
        }
    }

    fun deleteSnapshot(snapshot: Snapshot) {
        viewModelScope.launch(dispatcherIO) {
            try {
                deleteSnapshotUseCase.invoke(snapshot)
                withContext(Dispatchers.Main) {
                    fetchSnapshots()
                }
            } catch (e: Exception) {
                _snapshotsState.value = HomeState.Failure(e.localizedMessage)
            }
        }
    }
}