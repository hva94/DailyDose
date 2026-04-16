package com.hvasoft.dailydose.data.repository

import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.FeedSyncStateEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedItemEntity
import com.hvasoft.dailydose.data.local.OfflineItemAvailabilityStatus
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
import com.hvasoft.dailydose.data.local.OfflineMediaAssetEntity
import com.hvasoft.dailydose.data.local.OfflineMediaAssetType
import com.hvasoft.dailydose.data.local.OfflineMediaDownloadStatus
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.di.DispatcherIO
import com.hvasoft.dailydose.domain.common.extension_functions.isLikedBy
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import javax.inject.Inject

class HomeFeedRefreshCoordinator @Inject constructor(
    private val remoteDatabaseService: RemoteDatabaseService,
    private val offlineFeedItemDao: OfflineFeedItemDao,
    private val offlineMediaAssetDao: OfflineMediaAssetDao,
    private val feedSyncStateDao: FeedSyncStateDao,
    private val feedAssetStorage: FeedAssetStorage,
    @DispatcherIO private val dispatcherIO: CoroutineDispatcher,
) {

    suspend fun refresh(accountId: String): Result<Unit> = withContext(dispatcherIO) {
        val refreshTimestamp = System.currentTimeMillis()
        val previousSyncState = feedSyncStateDao.get(accountId)

        try {
            val ownerInfoCache = mutableMapOf<String, OwnerInfo>()
            val retainedAssetsById = linkedMapOf<String, OfflineMediaAssetEntity>()
            val retainedFeedItems = remoteDatabaseService.getSnapshotsOnce()
                .sortedByDescending { it.dateTime ?: 0L }
                .take(Constants.OFFLINE_FEED_RETENTION_LIMIT)
                .mapIndexed { index, snapshot ->
                    val snapshotId = snapshot.snapshotKey.ifBlank { "snapshot_$index" }
                    val ownerUserId = snapshot.idUserOwner.orEmpty()
                    val ownerInfo = ownerInfoCache.getOrPut(ownerUserId) {
                        OwnerInfo(
                            displayName = remoteDatabaseService.getUserNameOnce(ownerUserId),
                            avatarUrl = remoteDatabaseService.getUserPhotoUrlOnce(ownerUserId),
                        )
                    }

                    val mainImageAssetId = "snapshot-$accountId-$snapshotId"
                    val mainImageAsset = feedAssetStorage.retainRemoteAsset(
                        accountId = accountId,
                        assetId = mainImageAssetId,
                        assetType = OfflineMediaAssetType.SNAPSHOT_IMAGE,
                        sourceUrl = snapshot.photoUrl.orEmpty(),
                        referencedAt = refreshTimestamp,
                    )
                    retainedAssetsById[mainImageAssetId] = mainImageAsset

                    val avatarAssetId = ownerInfo.avatarUrl
                        .takeIf(String::isNotBlank)
                        ?.let { "avatar-$accountId-$ownerUserId" }
                    val avatarAsset = avatarAssetId?.let { assetId ->
                        retainedAssetsById.getOrPut(assetId) {
                            feedAssetStorage.retainRemoteAsset(
                                accountId = accountId,
                                assetId = assetId,
                                assetType = OfflineMediaAssetType.USER_AVATAR,
                                sourceUrl = ownerInfo.avatarUrl,
                                referencedAt = refreshTimestamp,
                            )
                        }
                    }

                    OfflineFeedItemEntity(
                        accountId = accountId,
                        snapshotId = snapshotId,
                        ownerUserId = ownerUserId,
                        title = snapshot.title.orEmpty(),
                        publishedAt = snapshot.dateTime ?: refreshTimestamp,
                        sortOrder = index.toLong(),
                        remotePhotoUrl = snapshot.photoUrl.orEmpty(),
                        mainImageAssetId = mainImageAssetId,
                        ownerDisplayName = ownerInfo.displayName,
                        ownerAvatarRemoteUrl = ownerInfo.avatarUrl,
                        ownerAvatarAssetId = avatarAsset?.assetId,
                        likeCount = snapshot.likeList?.size ?: snapshot.likeCount.toIntOrNull() ?: 0,
                        likedByCurrentUser = snapshot.isLikedBy(accountId),
                        availabilityStatus = if (mainImageAsset.downloadStatus == OfflineMediaDownloadStatus.READY) {
                            OfflineItemAvailabilityStatus.FULLY_AVAILABLE
                        } else {
                            OfflineItemAvailabilityStatus.MEDIA_PARTIAL
                        },
                        syncedAt = refreshTimestamp,
                    )
                }

            val existingAssets = offlineMediaAssetDao.getByAccount(accountId)
            offlineFeedItemDao.deleteByAccount(accountId)
            if (retainedFeedItems.isNotEmpty()) {
                offlineFeedItemDao.upsertAll(retainedFeedItems)
            }

            val retainedAssets = retainedAssetsById.values.toList()
            if (retainedAssets.isNotEmpty()) {
                offlineMediaAssetDao.upsertAll(retainedAssets)
            }

            val staleAssets = existingAssets.filter { existing ->
                retainedAssetsById.containsKey(existing.assetId).not()
            }
            if (staleAssets.isNotEmpty()) {
                offlineMediaAssetDao.deleteByIds(staleAssets.map(OfflineMediaAssetEntity::assetId))
                feedAssetStorage.deleteFiles(staleAssets.mapNotNull(OfflineMediaAssetEntity::localPath))
                logger.info("Removed ${staleAssets.size} stale offline assets for account $accountId")
            }

            val retainedItemCount = retainedFeedItems.size
            feedSyncStateDao.upsert(
                FeedSyncStateEntity(
                    accountId = accountId,
                    lastSuccessfulSyncAt = refreshTimestamp,
                    lastRefreshAttemptAt = refreshTimestamp,
                    lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
                    retainedItemCount = retainedItemCount,
                    retentionLimit = Constants.OFFLINE_FEED_RETENTION_LIMIT,
                    hasRetainedContent = retainedItemCount > 0,
                ),
            )

            Result.success(Unit)
        } catch (exception: Exception) {
            logger.warning("Offline feed refresh failed for account $accountId: ${exception.message}")
            val retainedItemCount = offlineFeedItemDao.countByAccount(accountId)
            feedSyncStateDao.upsert(
                FeedSyncStateEntity(
                    accountId = accountId,
                    lastSuccessfulSyncAt = previousSyncState?.lastSuccessfulSyncAt,
                    lastRefreshAttemptAt = refreshTimestamp,
                    lastRefreshResult = HomeFeedLastRefreshResult.NETWORK_FAILURE,
                    retainedItemCount = retainedItemCount,
                    retentionLimit = previousSyncState?.retentionLimit
                        ?: Constants.OFFLINE_FEED_RETENTION_LIMIT,
                    hasRetainedContent = retainedItemCount > 0,
                ),
            )
            Result.failure(exception)
        }
    }

    suspend fun clearAccount(accountId: String) = withContext(dispatcherIO) {
        val retainedAssets = offlineMediaAssetDao.getByAccount(accountId)
        offlineFeedItemDao.deleteByAccount(accountId)
        offlineMediaAssetDao.deleteByAccount(accountId)
        feedSyncStateDao.deleteByAccount(accountId)
        feedAssetStorage.deleteFiles(retainedAssets.mapNotNull(OfflineMediaAssetEntity::localPath))
        feedAssetStorage.clearAccount(accountId)
    }

    private data class OwnerInfo(
        val displayName: String,
        val avatarUrl: String,
    )

    private companion object {
        val logger: Logger = Logger.getLogger(HomeFeedRefreshCoordinator::class.java.name)
    }
}
