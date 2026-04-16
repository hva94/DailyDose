package com.hvasoft.dailydose.domain.model

data class HomeFeedSyncState(
    val availabilityMode: HomeFeedAvailabilityMode = HomeFeedAvailabilityMode.OFFLINE_EMPTY,
    val lastSuccessfulSyncAt: Long? = null,
    val lastRefreshAttemptAt: Long? = null,
    val lastRefreshResult: HomeFeedLastRefreshResult = HomeFeedLastRefreshResult.NEVER_SYNCED,
    val retainedItemCount: Int = 0,
    val retentionLimit: Int = DEFAULT_RETENTION_LIMIT,
    val hasRetainedContent: Boolean = false,
) {
    companion object {
        const val DEFAULT_RETENTION_LIMIT = 50
    }
}

enum class HomeFeedLastRefreshResult {
    SUCCESS,
    NETWORK_FAILURE,
    AUTH_FAILURE,
    UNKNOWN_FAILURE,
    NEVER_SYNCED,
}
