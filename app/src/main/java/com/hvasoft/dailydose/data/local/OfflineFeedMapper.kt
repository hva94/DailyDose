package com.hvasoft.dailydose.data.local

import com.hvasoft.dailydose.domain.model.HomeFeedAvailabilityMode
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.Snapshot
import javax.inject.Inject

class OfflineFeedMapper @Inject constructor() {

    fun toDomain(item: OfflineFeedItemWithAssets): Snapshot = Snapshot(
        title = item.title,
        dateTime = item.publishedAt,
        photoUrl = item.remotePhotoUrl,
        idUserOwner = item.ownerUserId,
        snapshotKey = item.snapshotId,
        userName = item.ownerDisplayName,
        userPhotoUrl = item.ownerAvatarRemoteUrl,
        isLikedByCurrentUser = item.likedByCurrentUser,
        likeCount = item.likeCount.toString(),
        localPhotoPath = item.mainLocalPath,
        localUserPhotoPath = item.avatarLocalPath,
        isOfflineMediaPartial = item.availabilityStatus == OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
        syncedAt = item.syncedAt,
    )

    fun toSyncState(entity: FeedSyncStateEntity?): HomeFeedSyncState {
        if (entity == null) {
            return HomeFeedSyncState(
                availabilityMode = HomeFeedAvailabilityMode.OFFLINE_EMPTY,
                lastRefreshResult = HomeFeedLastRefreshResult.NEVER_SYNCED,
                hasRetainedContent = false,
                retainedItemCount = 0,
            )
        }

        val availabilityMode = when {
            entity.lastRefreshResult == HomeFeedLastRefreshResult.SUCCESS ->
                HomeFeedAvailabilityMode.ONLINE_FRESH

            entity.hasRetainedContent -> HomeFeedAvailabilityMode.OFFLINE_RETAINED
            else -> HomeFeedAvailabilityMode.OFFLINE_EMPTY
        }

        return HomeFeedSyncState(
            availabilityMode = availabilityMode,
            lastSuccessfulSyncAt = entity.lastSuccessfulSyncAt,
            lastRefreshAttemptAt = entity.lastRefreshAttemptAt,
            lastRefreshResult = entity.lastRefreshResult,
            retainedItemCount = entity.retainedItemCount,
            retentionLimit = entity.retentionLimit,
            hasRetainedContent = entity.hasRetainedContent,
        )
    }
}
