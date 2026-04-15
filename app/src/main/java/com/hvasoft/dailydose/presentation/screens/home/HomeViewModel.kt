package com.hvasoft.dailydose.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hvasoft.dailydose.di.DispatcherIO
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCase
import com.hvasoft.dailydose.domain.model.Snapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    @DispatcherIO private val dispatcherIO: CoroutineDispatcher,
    private val getSnapshotsUseCase: GetSnapshotsUseCase,
    private val toggleUserLikeUseCase: ToggleUserLikeUseCase,
    private val deleteSnapshotUseCase: DeleteSnapshotUseCase
) : ViewModel() {

    private val refreshSignal = MutableStateFlow(0)
    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    @OptIn(ExperimentalPagingApi::class)
    val snapshots: Flow<PagingData<Snapshot>> = refreshSignal
        .flatMapLatest {
            flow { emitAll(getSnapshotsUseCase.invoke()) }
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalPagingApi::class)
    fun fetchSnapshots() {
        refreshSignal.value += 1
    }

    fun setLikeSnapshot(snapshot: Snapshot, isChecked: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            runCatching {
                toggleUserLikeUseCase.invoke(snapshot, isChecked)
            }.onFailure {
                _events.tryEmit(R.string.error_unknown)
            }
        }
    }

    fun deleteSnapshot(snapshot: Snapshot) {
        viewModelScope.launch(dispatcherIO) {
            runCatching {
                deleteSnapshotUseCase.invoke(snapshot)
            }.onSuccess {
                fetchSnapshots()
            }.onFailure {
                _events.tryEmit(R.string.error_unknown)
            }
        }
    }
}
