package com.hvasoft.dailydose.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.di.DispatcherIO
import com.hvasoft.dailydose.domain.common.extension_functions.canRevealImage
import com.hvasoft.dailydose.domain.common.extension_functions.canUseInteractions
import com.hvasoft.dailydose.domain.interactor.home.AddSnapshotReplyUseCase
import com.hvasoft.dailydose.domain.interactor.home.CachePostedSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.DeleteSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetActiveDailyPromptUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotRepliesUseCase
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import com.hvasoft.dailydose.domain.interactor.home.ObservePromptCompletionUseCase
import com.hvasoft.dailydose.domain.interactor.home.RevealSnapshotUseCase
import com.hvasoft.dailydose.domain.interactor.home.SetSnapshotReactionUseCase
import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.model.DailyPromptDay
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    @DispatcherIO private val dispatcherIO: CoroutineDispatcher,
    private val getSnapshotsUseCase: GetSnapshotsUseCase,
    private val getActiveDailyPromptUseCase: GetActiveDailyPromptUseCase,
    private val observePromptCompletionUseCase: ObservePromptCompletionUseCase,
    private val getSnapshotRepliesUseCase: GetSnapshotRepliesUseCase,
    private val addSnapshotReplyUseCase: AddSnapshotReplyUseCase,
    private val cachePostedSnapshotUseCase: CachePostedSnapshotUseCase,
    private val revealSnapshotUseCase: RevealSnapshotUseCase,
    private val setSnapshotReactionUseCase: SetSnapshotReactionUseCase,
    private val deleteSnapshotUseCase: DeleteSnapshotUseCase,
    private val authSessionProvider: AuthSessionProvider,
) : ViewModel() {

    private val sourceSignal = MutableStateFlow(0)
    private val isRefreshInFlight = MutableStateFlow(false)
    private val isRefreshIndicatorVisible = MutableStateFlow(false)
    private val _replySheetState = MutableStateFlow(HomeReplySheetUiState())
    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    private val _postPublishScrollSignal = MutableStateFlow(0)
    private val localPostedPromptDateKey = MutableStateFlow<String?>(null)
    val events = _events.asSharedFlow()
    val replySheetState = _replySheetState
    val postPublishScrollSignal = _postPublishScrollSignal

    @OptIn(ExperimentalPagingApi::class)
    val snapshots: Flow<PagingData<Snapshot>> = sourceSignal
        .flatMapLatest {
            flow { emitAll(getSnapshotsUseCase.invoke()) }
        }
        .cachedIn(viewModelScope)

    val uiState = combine(
        sourceSignal.flatMapLatest {
            getSnapshotsUseCase.observeSyncState()
        },
        isRefreshInFlight,
        isRefreshIndicatorVisible,
        sourceSignal.flatMapLatest { observePromptUiInputs() },
    ) { syncState, refreshing, showsRefreshIndicator, promptUiInputs ->
        HomeFeedUiState.from(
            syncState = syncState,
            isBackgroundRefreshing = refreshing,
            showsRefreshIndicator = showsRefreshIndicator,
            activeDailyPrompt = promptUiInputs.activeDailyPrompt,
            hasPostedToday = promptUiInputs.hasPostedToday,
            isPromptLoading = promptUiInputs.isPromptLoading,
        )
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
            localPostedPromptDateKey.value = DailyPromptDay.currentDateKey(
                snapshot.dateTime ?: System.currentTimeMillis(),
            )
            cachePostedSnapshotUseCase.invoke(snapshot)
            _postPublishScrollSignal.value += 1
        }
    }

    fun shouldAutoRefreshOnResume(nowMillis: Long): Boolean {
        if (authSessionProvider.currentUserIdOrNull() == null) return false
        if (uiState.value.isBackgroundRefreshing) return false
        val lastSuccessfulSyncAt = uiState.value.lastSuccessfulSyncAt ?: return false
        return nowMillis - lastSuccessfulSyncAt >= AUTO_REFRESH_STALE_MS
    }

    fun currentUserIdOrNull(): String? = authSessionProvider.currentUserIdOrNull()

    fun revealSnapshot(snapshot: Snapshot) {
        val currentUserId = currentUserIdOrNull()
        if (!snapshot.canRevealImage(currentUserId)) return
        viewModelScope.launch(dispatcherIO) {
            runCatching {
                revealSnapshotUseCase.invoke(snapshot)
            }.onFailure {
                _events.tryEmit(R.string.error_unknown)
            }
        }
    }

    fun setSnapshotReaction(snapshot: Snapshot, emoji: String?) {
        if (!snapshot.canUseInteractions(currentUserIdOrNull())) return
        viewModelScope.launch(dispatcherIO) {
            runCatching {
                setSnapshotReactionUseCase.invoke(snapshot, emoji)
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

    fun openReplies(snapshot: Snapshot) {
        if (!snapshot.canUseInteractions(currentUserIdOrNull())) return
        _replySheetState.value = HomeReplySheetUiState(
            isVisible = true,
            snapshot = snapshot,
            isLoading = true,
        )
        loadReplies(snapshot)
    }

    fun closeReplies() {
        _replySheetState.value = HomeReplySheetUiState()
    }

    fun retryReplies() {
        _replySheetState.value.snapshot?.let(::loadReplies)
    }

    fun updateReplyComposer(text: String) {
        _replySheetState.update { current ->
            current.copy(
                composerText = text,
                composerMessageRes = null,
            )
        }
    }

    fun submitReply() {
        val snapshot = _replySheetState.value.snapshot ?: return
        viewModelScope.launch(dispatcherIO) {
            _replySheetState.update { current ->
                current.copy(
                    isSubmitting = true,
                    composerMessageRes = null,
                )
            }

            addSnapshotReplyUseCase.invoke(snapshot, _replySheetState.value.composerText)
                .onSuccess {
                    _replySheetState.update { current ->
                        current.copy(
                            composerText = "",
                            isSubmitting = false,
                            composerMessageRes = null,
                        )
                    }
                    loadReplies(snapshot)
                }
                .onFailure { failure ->
                    val messageRes = when ((failure as? IllegalArgumentException)?.message) {
                        "blank" -> R.string.home_reply_blank_error
                        "length" -> R.string.home_reply_length_error
                        else -> R.string.error_unknown
                    }
                    _replySheetState.update { current ->
                        current.copy(
                            isSubmitting = false,
                            composerMessageRes = messageRes,
                        )
                    }
                    if (messageRes == R.string.error_unknown) {
                        _events.tryEmit(messageRes)
                    }
                }
        }
    }

    fun clearOfflineSnapshots(accountId: String) {
        viewModelScope.launch(dispatcherIO) {
            closeReplies()
            localPostedPromptDateKey.value = null
            getSnapshotsUseCase.clearOfflineSnapshots(accountId)
            sourceSignal.value += 1
        }
    }

    private fun loadReplies(snapshot: Snapshot) {
        viewModelScope.launch(dispatcherIO) {
            _replySheetState.update { current ->
                current.copy(
                    isVisible = true,
                    snapshot = snapshot,
                    isLoading = true,
                    errorMessageRes = null,
                )
            }

            getSnapshotRepliesUseCase.invoke(snapshot)
                .onSuccess { replies ->
                    _replySheetState.update { current ->
                        current.copy(
                            snapshot = snapshot,
                            replies = replies,
                            isLoading = false,
                            errorMessageRes = null,
                        )
                    }
                }
                .onFailure {
                    _replySheetState.update { current ->
                        current.copy(
                            snapshot = snapshot,
                            isLoading = false,
                            errorMessageRes = R.string.home_reply_load_error,
                        )
                    }
                }
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
                _replySheetState.value.snapshot?.let(::loadReplies)
            }
        }
    }

    private enum class RefreshTrigger {
        AUTO,
        MANUAL,
    }

    private fun observePromptUiInputs() = combine(
        getActiveDailyPromptUseCase.invoke()
            .map { prompt ->
                PromptLoadState(
                    activeDailyPrompt = prompt,
                    hasLoaded = true,
                )
            }
            .onStart { emit(PromptLoadState()) },
        observePromptCompletionUseCase.invoke()
            .map { hasPostedToday ->
                PostingCompletionState(
                    hasPostedToday = hasPostedToday,
                    hasLoaded = true,
                )
            }
            .onStart { emit(PostingCompletionState()) },
        localPostedPromptDateKey,
    ) { promptLoadState, completionState, locallyPostedDateKey ->
        val promptDayKey = promptLoadState.activeDailyPrompt?.dateKey ?: DailyPromptDay.currentDateKey()
        PromptUiInputs(
            activeDailyPrompt = promptLoadState.activeDailyPrompt,
            hasPostedToday = completionState.hasPostedToday ||
                locallyPostedDateKey == promptDayKey,
            isPromptLoading = !promptLoadState.hasLoaded || !completionState.hasLoaded,
        )
    }

    private companion object {
        const val AUTO_REFRESH_STALE_MS = 10 * 60 * 1000L
        const val AUTO_REFRESH_INDICATOR_DELAY_MS = 1_000L
    }

    private data class PromptUiInputs(
        val activeDailyPrompt: DailyPromptAssignment? = null,
        val hasPostedToday: Boolean = false,
        val isPromptLoading: Boolean = true,
    )

    private data class PromptLoadState(
        val activeDailyPrompt: DailyPromptAssignment? = null,
        val hasLoaded: Boolean = false,
    )

    private data class PostingCompletionState(
        val hasPostedToday: Boolean = false,
        val hasLoaded: Boolean = false,
    )
}
