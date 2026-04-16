package com.hvasoft.dailydose.presentation.screens.home

import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState

data class HomeFeedUiState(
    val availabilityMode: HomeFeedAvailabilityMode = HomeFeedAvailabilityMode.OFFLINE_EMPTY,
    val lastSuccessfulSyncAt: Long? = null,
    val lastRefreshResult: HomeFeedLastRefreshResult = HomeFeedLastRefreshResult.NEVER_SYNCED,
    val isBackgroundRefreshing: Boolean = false,
    val showsRefreshIndicator: Boolean = false,
    val actionPolicy: HomeFeedActionPolicy = HomeFeedActionPolicy.READ_ONLY_OFFLINE,
    val hasRetainedContent: Boolean = false,
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
            )
    }
}

enum class HomeFeedActionPolicy {
    FULL_ACCESS,
    READ_ONLY_OFFLINE,
}
