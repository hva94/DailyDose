package com.hvasoft.dailydose.data.repository

import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.FeedSyncStateEntity
import com.hvasoft.dailydose.data.local.HomeFeedTransactionRunner
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedItemEntity
import com.hvasoft.dailydose.data.local.OfflineItemAvailabilityStatus
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
import com.hvasoft.dailydose.data.local.OfflineMediaAssetEntity
import com.hvasoft.dailydose.data.local.OfflineMediaAssetType
import com.hvasoft.dailydose.data.local.OfflineMediaDownloadStatus
import com.hvasoft.dailydose.data.local.OfflineSnapshotReplyDao
import com.hvasoft.dailydose.data.local.ProfileLocalCache
import com.hvasoft.dailydose.data.local.toOfflineEntity
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.di.DispatcherIO
import com.hvasoft.dailydose.domain.common.extension_functions.normalizedCurrentUserReaction
import com.hvasoft.dailydose.domain.common.extension_functions.normalizedReactionCount
import com.hvasoft.dailydose.domain.common.extension_functions.normalizedReactionSummary
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.logging.Logger
import javax.inject.Inject

class HomeFeedRefreshCoordinator @Inject constructor(
    private val remoteDatabaseService: RemoteDatabaseService,
    private val transactionRunner: HomeFeedTransactionRunner,
    private val offlineFeedItemDao: OfflineFeedItemDao,
    private val offlineMediaAssetDao: OfflineMediaAssetDao,
    private val offlineSnapshotReplyDao: OfflineSnapshotReplyDao,
    private val feedSyncStateDao: FeedSyncStateDao,
    private val feedAssetStorage: FeedAssetStorage,
    private val profileLocalCache: ProfileLocalCache,
    @DispatcherIO private val dispatcherIO: CoroutineDispatcher,
) {

    suspend fun refresh(accountId: String): Result<Unit> = withContext(dispatcherIO) {
        val refreshTimestamp = System.currentTimeMillis()
        val previousSyncState = feedSyncStateDao.get(accountId)

        try {
            val existingFeedItems = offlineFeedItemDao.getByAccount(accountId)
            val existingFeedItemsBySnapshotId = existingFeedItems.associateBy(OfflineFeedItemEntity::snapshotId)
            val existingAssets = offlineMediaAssetDao.getByAccount(accountId)
            val existingAssetsById = existingAssets.associateBy(OfflineMediaAssetEntity::assetId)
            val remoteSnapshots = remoteDatabaseService.getSnapshotsOnce()
                .sortedByDescending { it.dateTime ?: 0L }
                .take(Constants.OFFLINE_FEED_RETENTION_LIMIT)
            val ownerInfoByUserId = loadOwnerInfoByUserId(
                remoteSnapshots = remoteSnapshots,
                existingFeedItemsBySnapshotId = existingFeedItemsBySnapshotId,
            )
            val desiredRetainedItems = remoteSnapshots.mapIndexed { index, snapshot ->
                buildDesiredRetainedItem(
                    accountId = accountId,
                    snapshot = snapshot,
                    sortOrder = index.toLong(),
                    refreshTimestamp = refreshTimestamp,
                    ownerInfo = ownerInfoByUserId[snapshot.idUserOwner.orEmpty()] ?: OwnerInfo(),
                    existingItem = existingFeedItemsBySnapshotId[snapshot.snapshotKey.ifBlank { "snapshot_$index" }],
                )
            }
            val desiredAssetsById = linkedMapOf<String, DesiredAsset>()
            desiredRetainedItems.forEach { desiredItem ->
                desiredAssetsById[desiredItem.mainImageAsset.assetId] = desiredItem.mainImageAsset
                desiredItem.avatarAsset?.let { avatarAsset ->
                    desiredAssetsById[avatarAsset.assetId] = avatarAsset
                }
            }
            val repliesBySnapshotId = loadRepliesBySnapshotId(remoteSnapshots)

            if (canUseFastPath(desiredRetainedItems, existingFeedItems, existingAssetsById)) {
                syncRetainedReplies(
                    accountId = accountId,
                    retainedSnapshotIds = desiredRetainedItems.map(DesiredRetainedItem::snapshotId),
                    repliesBySnapshotId = repliesBySnapshotId,
                )
                syncCurrentUserProfileCache(
                    accountId = accountId,
                    retainedItems = existingFeedItems,
                    retainedAssetsById = existingAssetsById,
                )
                feedSyncStateDao.upsert(
                    buildSuccessfulSyncState(
                        accountId = accountId,
                        refreshTimestamp = refreshTimestamp,
                        retainedItemCount = desiredRetainedItems.size,
                        previousSyncState = previousSyncState,
                    ),
                )
                return@withContext Result.success(Unit)
            }

            val retainedAssetsById = linkedMapOf<String, OfflineMediaAssetEntity>()
            desiredAssetsById.values.forEach { desiredAsset ->
                retainedAssetsById[desiredAsset.assetId] = feedAssetStorage.retainRemoteAsset(
                    accountId = accountId,
                    assetId = desiredAsset.assetId,
                    assetType = desiredAsset.assetType,
                    sourceUrl = desiredAsset.sourceUrl,
                    referencedAt = refreshTimestamp,
                    existingAsset = existingAssetsById[desiredAsset.assetId],
                )
            }
            val retainedFeedItems = desiredRetainedItems.map { desiredItem ->
                desiredItem.toEntity(
                    mainImageAsset = retainedAssetsById.getValue(desiredItem.mainImageAsset.assetId),
                    refreshTimestamp = refreshTimestamp,
                )
            }
            syncCurrentUserProfileCache(
                accountId = accountId,
                retainedItems = retainedFeedItems,
                retainedAssetsById = retainedAssetsById,
            )
            val retainedAssets = retainedAssetsById.values.toList()
            val staleAssets = existingAssets.filter { existing ->
                desiredAssetsById.containsKey(existing.assetId).not()
            }
            val staleSnapshotIds = existingFeedItems
                .map(OfflineFeedItemEntity::snapshotId)
                .filter { snapshotId -> desiredRetainedItems.none { it.snapshotId == snapshotId } }
            val retainedItemCount = retainedFeedItems.size
            transactionRunner.runInTransaction {
                if (retainedFeedItems.isEmpty()) {
                    offlineFeedItemDao.deleteByAccount(accountId)
                } else {
                    offlineFeedItemDao.upsertAll(retainedFeedItems)
                    offlineFeedItemDao.deleteMissingSnapshots(
                        accountId = accountId,
                        snapshotIds = retainedFeedItems.map(OfflineFeedItemEntity::snapshotId),
                    )
                }
                syncRetainedReplies(
                    accountId = accountId,
                    retainedSnapshotIds = retainedFeedItems.map(OfflineFeedItemEntity::snapshotId),
                    repliesBySnapshotId = repliesBySnapshotId,
                )
                staleSnapshotIds.forEach { snapshotId ->
                    offlineSnapshotReplyDao.deleteBySnapshot(accountId, snapshotId)
                }

                if (retainedAssets.isEmpty()) {
                    offlineMediaAssetDao.deleteByAccount(accountId)
                } else {
                    offlineMediaAssetDao.upsertAll(retainedAssets)
                    offlineMediaAssetDao.deleteMissingAssets(
                        accountId = accountId,
                        assetIds = retainedAssets.map(OfflineMediaAssetEntity::assetId),
                    )
                }

                feedSyncStateDao.upsert(
                    buildSuccessfulSyncState(
                        accountId = accountId,
                        refreshTimestamp = refreshTimestamp,
                        retainedItemCount = retainedItemCount,
                        previousSyncState = previousSyncState,
                    ),
                )
            }

            if (staleAssets.isNotEmpty()) {
                feedAssetStorage.deleteFiles(staleAssets.mapNotNull(OfflineMediaAssetEntity::localPath))
                logger.info("Removed ${staleAssets.size} stale offline assets for account $accountId")
            }

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
        offlineSnapshotReplyDao.deleteByAccount(accountId)
        feedSyncStateDao.deleteByAccount(accountId)
        feedAssetStorage.deleteFiles(retainedAssets.mapNotNull(OfflineMediaAssetEntity::localPath))
        feedAssetStorage.clearAccount(accountId)
    }

    private data class OwnerInfo(
        val displayName: String = "",
        val avatarUrl: String = "",
    )

    private data class DesiredAsset(
        val assetId: String,
        val assetType: OfflineMediaAssetType,
        val sourceUrl: String,
    )

    private data class DesiredRetainedItem(
        val snapshotId: String,
        val ownerUserId: String,
        val title: String,
        val publishedAt: Long,
        val sortOrder: Long,
        val remotePhotoUrl: String,
        val mainImageAsset: DesiredAsset,
        val ownerDisplayName: String,
        val ownerAvatarRemoteUrl: String,
        val avatarAsset: DesiredAsset?,
        val likeCount: Int,
        val likedByCurrentUser: Boolean,
        val reactionCount: Int,
        val reactionSummary: Map<String, Int>,
        val currentUserReaction: String?,
        val replyCount: Int,
        val legacyLikeCount: Int?,
    ) {
        fun metadataMatches(existingItem: OfflineFeedItemEntity): Boolean = existingItem.copy(
            availabilityStatus = existingItem.availabilityStatus,
            syncedAt = existingItem.syncedAt,
            hasPendingReaction = existingItem.hasPendingReaction,
            hasPendingReply = existingItem.hasPendingReply,
        ) == OfflineFeedItemEntity(
            accountId = existingItem.accountId,
            snapshotId = snapshotId,
            ownerUserId = ownerUserId,
            title = title,
            publishedAt = publishedAt,
            sortOrder = sortOrder,
            remotePhotoUrl = remotePhotoUrl,
            mainImageAssetId = mainImageAsset.assetId,
            ownerDisplayName = ownerDisplayName,
            ownerAvatarRemoteUrl = ownerAvatarRemoteUrl,
            ownerAvatarAssetId = avatarAsset?.assetId,
            likeCount = likeCount,
            likedByCurrentUser = likedByCurrentUser,
            reactionCount = reactionCount,
            reactionSummary = reactionSummary,
            currentUserReaction = currentUserReaction,
            replyCount = replyCount,
            hasPendingReaction = existingItem.hasPendingReaction,
            hasPendingReply = existingItem.hasPendingReply,
            legacyLikeCount = legacyLikeCount,
            availabilityStatus = existingItem.availabilityStatus,
            syncedAt = existingItem.syncedAt,
        )

        fun toEntity(
            mainImageAsset: OfflineMediaAssetEntity,
            refreshTimestamp: Long,
        ): OfflineFeedItemEntity = OfflineFeedItemEntity(
            accountId = mainImageAsset.accountId,
            snapshotId = snapshotId,
            ownerUserId = ownerUserId,
            title = title,
            publishedAt = publishedAt,
            sortOrder = sortOrder,
            remotePhotoUrl = remotePhotoUrl,
            mainImageAssetId = mainImageAsset.assetId,
            ownerDisplayName = ownerDisplayName,
            ownerAvatarRemoteUrl = ownerAvatarRemoteUrl,
            ownerAvatarAssetId = avatarAsset?.assetId,
            likeCount = likeCount,
            likedByCurrentUser = likedByCurrentUser,
            reactionCount = reactionCount,
            reactionSummary = reactionSummary,
            currentUserReaction = currentUserReaction,
            replyCount = replyCount,
            hasPendingReaction = false,
            hasPendingReply = false,
            legacyLikeCount = legacyLikeCount,
            availabilityStatus = if (mainImageAsset.downloadStatus == OfflineMediaDownloadStatus.READY) {
                OfflineItemAvailabilityStatus.FULLY_AVAILABLE
            } else {
                OfflineItemAvailabilityStatus.MEDIA_PARTIAL
            },
            syncedAt = refreshTimestamp,
        )
    }

    private suspend fun loadOwnerInfoByUserId(
        remoteSnapshots: List<Snapshot>,
        existingFeedItemsBySnapshotId: Map<String, OfflineFeedItemEntity>,
    ): Map<String, OwnerInfo> {
        val ownerUserIds = remoteSnapshots.mapNotNull { snapshot ->
            snapshot.idUserOwner?.takeIf(String::isNotBlank)
        }.toSet()
        if (ownerUserIds.isEmpty()) return emptyMap()

        val usersById = runCatching {
            remoteDatabaseService.getUsersOnce(ownerUserIds)
        }.getOrElse { failure ->
            logger.warning(
                "Failed to load owner profiles for ${ownerUserIds.size} users. " +
                    "Falling back to cached snapshot identity. Cause: ${failure.message}",
            )
            emptyMap()
        }

        return ownerUserIds.associateWith { ownerUserId ->
            val sampleExistingItem = existingFeedItemsBySnapshotId.values.firstOrNull {
                it.ownerUserId == ownerUserId
            }
            val user = usersById[ownerUserId]
            OwnerInfo(
                displayName = user?.userName
                    ?.takeIf(String::isNotBlank)
                    ?: sampleExistingItem?.ownerDisplayName
                    .orEmpty(),
                avatarUrl = user?.photoUrl
                    ?.takeIf(String::isNotBlank)
                    ?: sampleExistingItem?.ownerAvatarRemoteUrl
                    .orEmpty(),
            )
        }
    }

    private fun buildDesiredRetainedItem(
        accountId: String,
        snapshot: Snapshot,
        sortOrder: Long,
        refreshTimestamp: Long,
        ownerInfo: OwnerInfo,
        existingItem: OfflineFeedItemEntity?,
    ): DesiredRetainedItem {
        val snapshotId = snapshot.snapshotKey.ifBlank { "snapshot_$sortOrder" }
        val ownerUserId = snapshot.idUserOwner.orEmpty()
        val resolvedOwnerDisplayName = ownerInfo.displayName
            .ifBlank { existingItem?.ownerDisplayName.orEmpty() }
            .ifBlank { snapshot.userName.orEmpty() }
        val resolvedAvatarUrl = ownerInfo.avatarUrl
            .ifBlank { existingItem?.ownerAvatarRemoteUrl.orEmpty() }
            .ifBlank { snapshot.userPhotoUrl.orEmpty() }
        val mainImageAssetId = "snapshot-$accountId-$snapshotId"
        val avatarAsset = resolvedAvatarUrl
            .takeIf(String::isNotBlank)
            ?.let { avatarUrl ->
                DesiredAsset(
                    assetId = "avatar-$accountId-$ownerUserId",
                    assetType = OfflineMediaAssetType.USER_AVATAR,
                    sourceUrl = avatarUrl,
                )
            }

        return DesiredRetainedItem(
            snapshotId = snapshotId,
            ownerUserId = ownerUserId,
            title = snapshot.title.orEmpty(),
            publishedAt = snapshot.dateTime ?: existingItem?.publishedAt ?: refreshTimestamp,
            sortOrder = sortOrder,
            remotePhotoUrl = snapshot.photoUrl.orEmpty(),
            mainImageAsset = DesiredAsset(
                assetId = mainImageAssetId,
                assetType = OfflineMediaAssetType.SNAPSHOT_IMAGE,
                sourceUrl = snapshot.photoUrl.orEmpty(),
            ),
            ownerDisplayName = resolvedOwnerDisplayName,
            ownerAvatarRemoteUrl = resolvedAvatarUrl,
            avatarAsset = avatarAsset,
            likeCount = snapshot.normalizedReactionCount(),
            likedByCurrentUser = snapshot.normalizedCurrentUserReaction(accountId) != null,
            reactionCount = snapshot.normalizedReactionCount(),
            reactionSummary = snapshot.normalizedReactionSummary(),
            currentUserReaction = snapshot.normalizedCurrentUserReaction(accountId),
            replyCount = snapshot.replyCount,
            legacyLikeCount = snapshot.likeList?.size,
        )
    }

    private suspend fun loadRepliesBySnapshotId(
        remoteSnapshots: List<Snapshot>,
    ): Map<String, List<com.hvasoft.dailydose.domain.model.SnapshotReply>> = coroutineScope {
        remoteSnapshots.map { snapshot ->
            async {
                snapshot.snapshotKey to runCatching {
                    remoteDatabaseService.getSnapshotReplies(snapshot.snapshotKey)
                }.getOrElse { failure ->
                    logger.warning(
                        "Failed to retain replies for snapshot ${snapshot.snapshotKey}: ${failure.message}",
                    )
                    emptyList()
                }
            }
        }.awaitAll().toMap()
    }

    private suspend fun syncRetainedReplies(
        accountId: String,
        retainedSnapshotIds: List<String>,
        repliesBySnapshotId: Map<String, List<com.hvasoft.dailydose.domain.model.SnapshotReply>>,
    ) {
        retainedSnapshotIds.forEach { snapshotId ->
            offlineSnapshotReplyDao.deleteBySnapshotAndDeliveryState(
                accountId = accountId,
                snapshotId = snapshotId,
                deliveryState = SnapshotReplyDeliveryState.CONFIRMED,
            )
            val replies = repliesBySnapshotId[snapshotId].orEmpty()
            if (replies.isNotEmpty()) {
                offlineSnapshotReplyDao.upsertAll(
                    replies.map { reply ->
                        reply.copy(deliveryState = SnapshotReplyDeliveryState.CONFIRMED)
                            .toOfflineEntity(accountId)
                    },
                )
            }
        }
    }

    private fun syncCurrentUserProfileCache(
        accountId: String,
        retainedItems: List<OfflineFeedItemEntity>,
        retainedAssetsById: Map<String, OfflineMediaAssetEntity>,
    ) {
        val currentUserItem = retainedItems.firstOrNull { it.ownerUserId == accountId } ?: return
        val existingProfile = profileLocalCache.get(accountId)
        val localPhotoPath = currentUserItem.ownerAvatarAssetId
            ?.let(retainedAssetsById::get)
            ?.localPath
            ?.takeIf(String::isNotBlank)
            ?: existingProfile?.localPhotoPath.orEmpty()
        profileLocalCache.save(
            userId = accountId,
            displayName = currentUserItem.ownerDisplayName.ifBlank {
                existingProfile?.displayName.orEmpty()
            },
            photoUrl = currentUserItem.ownerAvatarRemoteUrl.ifBlank {
                existingProfile?.photoUrl.orEmpty()
            },
            localPhotoPath = localPhotoPath,
            email = existingProfile?.email.orEmpty(),
        )
    }

    private fun canUseFastPath(
        desiredRetainedItems: List<DesiredRetainedItem>,
        existingFeedItems: List<OfflineFeedItemEntity>,
        existingAssetsById: Map<String, OfflineMediaAssetEntity>,
    ): Boolean {
        if (desiredRetainedItems.size != existingFeedItems.size) return false

        val existingItemsBySnapshotId = existingFeedItems.associateBy(OfflineFeedItemEntity::snapshotId)
        val desiredAssetIds = linkedSetOf<String>()

        desiredRetainedItems.forEach { desiredItem ->
            val existingItem = existingItemsBySnapshotId[desiredItem.snapshotId] ?: return false
            if (desiredItem.metadataMatches(existingItem).not()) return false

            val mainImageReusable = isReusableAsset(
                desiredAsset = desiredItem.mainImageAsset,
                existingAsset = existingAssetsById[desiredItem.mainImageAsset.assetId],
            )
            if (mainImageReusable.not()) return false
            if (expectedAvailability(mainImageReusable, desiredItem.mainImageAsset.sourceUrl) != existingItem.availabilityStatus) {
                return false
            }

            desiredAssetIds += desiredItem.mainImageAsset.assetId
            desiredItem.avatarAsset?.let { avatarAsset ->
                if (isReusableAsset(avatarAsset, existingAssetsById[avatarAsset.assetId]).not()) {
                    return false
                }
                desiredAssetIds += avatarAsset.assetId
            }
        }

        return existingAssetsById.keys == desiredAssetIds
    }

    private fun isReusableAsset(
        desiredAsset: DesiredAsset,
        existingAsset: OfflineMediaAssetEntity?,
    ): Boolean {
        if (existingAsset == null) return false
        if (existingAsset.sourceUrl != desiredAsset.sourceUrl) return false
        if (desiredAsset.sourceUrl.isBlank()) {
            return existingAsset.localPath == null &&
                existingAsset.downloadStatus == OfflineMediaDownloadStatus.MISSING
        }

        return existingAsset.downloadStatus == OfflineMediaDownloadStatus.READY &&
            existingAsset.localPath
                ?.let(::File)
                ?.exists() == true
    }

    private fun expectedAvailability(
        mainImageReusable: Boolean,
        sourceUrl: String,
    ): OfflineItemAvailabilityStatus = if (mainImageReusable && sourceUrl.isNotBlank()) {
        OfflineItemAvailabilityStatus.FULLY_AVAILABLE
    } else {
        OfflineItemAvailabilityStatus.MEDIA_PARTIAL
    }

    private fun buildSuccessfulSyncState(
        accountId: String,
        refreshTimestamp: Long,
        retainedItemCount: Int,
        previousSyncState: FeedSyncStateEntity?,
    ): FeedSyncStateEntity = FeedSyncStateEntity(
        accountId = accountId,
        lastSuccessfulSyncAt = refreshTimestamp,
        lastRefreshAttemptAt = refreshTimestamp,
        lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
        retainedItemCount = retainedItemCount,
        retentionLimit = previousSyncState?.retentionLimit ?: Constants.OFFLINE_FEED_RETENTION_LIMIT,
        hasRetainedContent = retainedItemCount > 0,
    )

    private companion object {
        val logger: Logger = Logger.getLogger(HomeFeedRefreshCoordinator::class.java.name)
    }
}
