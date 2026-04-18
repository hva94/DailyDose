package com.hvasoft.dailydose.presentation.screens.home

import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply

data class HomeFeedUiState(
    val availabilityMode: HomeFeedAvailabilityMode = HomeFeedAvailabilityMode.OFFLINE_EMPTY,
    val lastSuccessfulSyncAt: Long? = null,
    val lastRefreshResult: HomeFeedLastRefreshResult = HomeFeedLastRefreshResult.NEVER_SYNCED,
    val isBackgroundRefreshing: Boolean = false,
    val showsRefreshIndicator: Boolean = false,
    val actionPolicy: HomeFeedActionPolicy = HomeFeedActionPolicy.READ_ONLY_OFFLINE,
    val hasRetainedContent: Boolean = false,
    val canQueueInteractions: Boolean = false,
) {
    val isInitialLoadInProgress: Boolean
        get() = isBackgroundRefreshing &&
            lastRefreshResult == HomeFeedLastRefreshResult.NEVER_SYNCED &&
            hasRetainedContent.not()

    val showsOfflineMessaging: Boolean
        get() = availabilityMode != HomeFeedAvailabilityMode.ONLINE_FRESH

    companion object {
        fun from(
            syncState: HomeFeedSyncState,
            isBackgroundRefreshing: Boolean,
            showsRefreshIndicator: Boolean,
        ): HomeFeedUiState =
            HomeFeedUiState(
                availabilityMode = syncState.availabilityMode,
                lastSuccessfulSyncAt = syncState.lastSuccessfulSyncAt,
                lastRefreshResult = syncState.lastRefreshResult,
                isBackgroundRefreshing = isBackgroundRefreshing,
                showsRefreshIndicator = showsRefreshIndicator,
                actionPolicy = if (syncState.availabilityMode == HomeFeedAvailabilityMode.ONLINE_FRESH) {
                    HomeFeedActionPolicy.FULL_ACCESS
                } else {
                    HomeFeedActionPolicy.READ_ONLY_OFFLINE
                },
                hasRetainedContent = syncState.hasRetainedContent,
                canQueueInteractions = syncState.hasRetainedContent,
            )
    }
}

enum class HomeFeedActionPolicy {
    FULL_ACCESS,
    READ_ONLY_OFFLINE,
}

data class HomeReplySheetUiState(
    val isVisible: Boolean = false,
    val snapshot: Snapshot? = null,
    val replies: List<SnapshotReply> = emptyList(),
    val composerText: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessageRes: Int? = null,
    val composerMessageRes: Int? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessageRes == null && replies.isEmpty()
}
