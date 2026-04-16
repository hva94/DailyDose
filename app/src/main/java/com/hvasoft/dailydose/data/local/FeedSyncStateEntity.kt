package com.hvasoft.dailydose.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult

@Entity(tableName = "feed_sync_state")
data class FeedSyncStateEntity(
    @PrimaryKey val accountId: String,
    val lastSuccessfulSyncAt: Long?,
    val lastRefreshAttemptAt: Long?,
    val lastRefreshResult: HomeFeedLastRefreshResult,
    val retainedItemCount: Int,
    val retentionLimit: Int,
    val hasRetainedContent: Boolean,
)
