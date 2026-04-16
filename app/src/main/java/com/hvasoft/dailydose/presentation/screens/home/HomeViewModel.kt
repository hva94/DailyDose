package com.hvasoft.dailydose.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.di.DispatcherIO
import com.hvasoft.dailydose.domain.interactor.home.CachePostedSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.ToggleUserLikeUseCase
import com.hvasoft.dailydose.domain.model.Snapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    @DispatcherIO private val dispatcherIO: CoroutineDispatcher,
    private val getSnapshotsUseCase: GetSnapshotsUseCase,
    private val cachePostedSnapshotUseCase: CachePostedSnapshotUseCase,
    private val toggleUserLikeUseCase: ToggleUserLikeUseCase,
    private val deleteSnapshotUseCase: DeleteSnapshotUseCase,
    private val authSessionProvider: AuthSessionProvider,
) : ViewModel() {

    private val sourceSignal = MutableStateFlow(0)
    private val isRefreshInFlight = MutableStateFlow(false)
    private val isRefreshIndicatorVisible = MutableStateFlow(false)
    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    @OptIn(ExperimentalPagingApi::class)
    val snapshots: Flow<PagingData<Snapshot>> = sourceSignal
        .flatMapLatest {
            flow { emitAll(getSnapshotsUseCase.invoke()) }
        }
        .cachedIn(viewModelScope)

    val uiState = sourceSignal
        .flatMapLatest {
            getSnapshotsUseCase.observeSyncState()
        }
        .combine(isRefreshInFlight) { syncState, refreshing ->
            syncState to refreshing
        }
        .combine(isRefreshIndicatorVisible) { (syncState, refreshing), showsRefreshIndicator ->
            HomeFeedUiState.from(syncState, refreshing, showsRefreshIndicator)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeFeedUiState(),
        )

    @OptIn(ExperimentalPagingApi::class)
    fun fetchSnapshots(reloadSource: Boolean = true) {
        if (authSessionProvider.currentUserIdOrNull() == null) return
        if (reloadSource) {
            sourceSignal.value += 1
        }
        refreshSnapshots(
            emitFailureMessage = false,
            refreshTrigger = RefreshTrigger.AUTO,
        )
    }

    fun retrySync() {
        if (authSessionProvider.currentUserIdOrNull() == null) return
        refreshSnapshots(
            emitFailureMessage = true,
            refreshTrigger = RefreshTrigger.MANUAL,
        )
    }

    fun cachePostedSnapshot(snapshot: Snapshot) {
        viewModelScope.launch(dispatcherIO) {
            cachePostedSnapshotUseCase.invoke(snapshot)
        }
    }

    fun shouldAutoRefreshOnResume(nowMillis: Long): Boolean {
        if (authSessionProvider.currentUserIdOrNull() == null) return false
        if (uiState.value.isBackgroundRefreshing) return false
        val lastSuccessfulSyncAt = uiState.value.lastSuccessfulSyncAt ?: return false
        return nowMillis - lastSuccessfulSyncAt >= AUTO_REFRESH_STALE_MS
    }

    fun currentUserIdOrNull(): String? = authSessionProvider.currentUserIdOrNull()

    fun setLikeSnapshot(snapshot: Snapshot, isChecked: Boolean) {
        if (uiState.value.actionPolicy == HomeFeedActionPolicy.READ_ONLY_OFFLINE) {
            _events.tryEmit(R.string.home_like_offline_unavailable)
            return
        }
        viewModelScope.launch(dispatcherIO) {
            runCatching {
                toggleUserLikeUseCase.invoke(snapshot, isChecked)
            }.onFailure {
                _events.tryEmit(R.string.error_unknown)
            }
        }
    }

    fun deleteSnapshot(snapshot: Snapshot) {
        if (uiState.value.actionPolicy == HomeFeedActionPolicy.READ_ONLY_OFFLINE) {
            _events.tryEmit(R.string.home_delete_offline_unavailable)
            return
        }
        viewModelScope.launch(dispatcherIO) {
            runCatching {
                deleteSnapshotUseCase.invoke(snapshot)
            }.onFailure {
                _events.tryEmit(R.string.error_unknown)
            }
        }
    }

    fun clearOfflineSnapshots(accountId: String) {
        viewModelScope.launch(dispatcherIO) {
            getSnapshotsUseCase.clearOfflineSnapshots(accountId)
            sourceSignal.value += 1
        }
    }

    private fun refreshSnapshots(
        emitFailureMessage: Boolean,
        refreshTrigger: RefreshTrigger,
    ) {
        if (isRefreshInFlight.value) return
        viewModelScope.launch(dispatcherIO) {
            isRefreshInFlight.value = true
            val indicatorJob = when (refreshTrigger) {
                RefreshTrigger.MANUAL -> {
                    isRefreshIndicatorVisible.value = true
                    null
                }

                RefreshTrigger.AUTO -> launch {
                    delay(AUTO_REFRESH_INDICATOR_DELAY_MS)
                    if (isRefreshInFlight.value) {
                        isRefreshIndicatorVisible.value = true
                    }
                }
            }

            try {
                getSnapshotsUseCase.refresh()
                    .onFailure {
                        if (emitFailureMessage) {
                            _events.tryEmit(R.string.error_connectivity)
                        }
                    }
            } finally {
                indicatorJob?.cancel()
                isRefreshIndicatorVisible.value = false
                isRefreshInFlight.value = false
            }
        }
    }

    private enum class RefreshTrigger {
        AUTO,
        MANUAL,
    }

    private companion object {
        const val AUTO_REFRESH_STALE_MS = 10 * 60 * 1000L
        const val AUTO_REFRESH_INDICATOR_DELAY_MS = 1_000L
    }
}
