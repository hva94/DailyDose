package com.hvasoft.dailydose.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.FeedAssetStorage
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.FeedSyncStateEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedItemEntity
import com.hvasoft.dailydose.data.local.OfflineFeedMapper
import com.hvasoft.dailydose.data.local.OfflineItemAvailabilityStatus
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
import com.hvasoft.dailydose.data.local.OfflineMediaAssetEntity
import com.hvasoft.dailydose.data.local.PendingSnapshotActionDao
import com.hvasoft.dailydose.data.local.PendingSnapshotActionEntity
import com.hvasoft.dailydose.domain.common.extension_functions.normalizedCurrentUserReaction
import com.hvasoft.dailydose.domain.common.extension_functions.normalizedReactionCount
import com.hvasoft.dailydose.domain.common.extension_functions.normalizedReactionSummary
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionQueueState
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionType
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val remoteDatabaseService: com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService,
    private val offlineFeedItemDao: OfflineFeedItemDao,
    private val offlineMediaAssetDao: OfflineMediaAssetDao,
    private val pendingSnapshotActionDao: PendingSnapshotActionDao,
    private val feedSyncStateDao: FeedSyncStateDao,
    private val offlineFeedMapper: OfflineFeedMapper,
    private val refreshCoordinator: HomeFeedRefreshCoordinator,
    private val interactionSyncCoordinator: SnapshotInteractionSyncCoordinator,
    private val authSessionProvider: AuthSessionProvider,
    private val feedAssetStorage: FeedAssetStorage,
) : HomeRepository {

    override suspend fun getPagedSnapshots(): Flow<PagingData<Snapshot>> {
        val accountId = authSessionProvider.currentUserIdOrNull() ?: return flowOf(PagingData.empty())
        return Pager(
            config = PagingConfig(pageSize = Constants.SNAPSHOTS_ITEMS_PER_PAGE),
            pagingSourceFactory = { offlineFeedItemDao.pagingSource(accountId) },
        ).flow.map { pagingData ->
            pagingData.map(offlineFeedMapper::toDomain)
        }
    }

    override fun observeSyncState(): Flow<HomeFeedSyncState> {
        val accountId = authSessionProvider.currentUserIdOrNull() ?: return flowOf(HomeFeedSyncState())
        return feedSyncStateDao.observe(accountId).map(offlineFeedMapper::toSyncState)
    }

    override suspend fun refreshSnapshots(): Result<Unit> {
        val accountId = authSessionProvider.currentUserIdOrNull()
            ?: return Result.success(Unit)
        val initialRefresh = refreshCoordinator.refresh(accountId)
        if (initialRefresh.isFailure) {
            return initialRefresh
        }

        val syncResult = interactionSyncCoordinator.flushPendingActions(
            accountId = accountId,
            rollbackOnFailure = true,
        )

        return if (syncResult.applied || syncResult.rolledBack) {
            refreshCoordinator.refresh(accountId)
        } else {
            initialRefresh
        }
    }

    override suspend fun clearOfflineSnapshots(accountId: String) {
        refreshCoordinator.clearAccount(accountId)
        pendingSnapshotActionDao.deleteByAccount(accountId)
    }

    override suspend fun cachePostedSnapshot(snapshot: Snapshot) {
        val accountId = authSessionProvider.currentUserIdOrNull() ?: return
        val currentUser = authSessionProvider.currentUserSnapshotOrNull()
        val cachedAt = snapshot.dateTime ?: System.currentTimeMillis()
        val ownerUserId = snapshot.idUserOwner ?: currentUser?.userId.orEmpty()
        val resolvedAvatar = resolveCurrentUserAvatar(
            accountId = accountId,
            ownerUserId = ownerUserId,
            snapshotPhotoUrl = snapshot.userPhotoUrl,
            authPhotoUrl = currentUser?.photoUrl.orEmpty(),
        )

        offlineFeedItemDao.incrementSortOrders(accountId)
        offlineFeedItemDao.upsertAll(
            listOf(
                OfflineFeedItemEntity(
                    accountId = accountId,
                    snapshotId = snapshot.snapshotKey,
                    ownerUserId = ownerUserId,
                    title = snapshot.title.orEmpty(),
                    publishedAt = cachedAt,
                    sortOrder = 0L,
                    remotePhotoUrl = snapshot.photoUrl.orEmpty(),
                    mainImageAssetId = null,
                    ownerDisplayName = snapshot.userName ?: currentUser?.displayName.orEmpty(),
                    ownerAvatarRemoteUrl = resolvedAvatar.remoteUrl,
                    ownerAvatarAssetId = resolvedAvatar.assetId,
                    likeCount = snapshot.likeCount.toIntOrNull() ?: 0,
                    likedByCurrentUser = snapshot.isLikedByCurrentUser,
                    reactionCount = snapshot.normalizedReactionCount(),
                    reactionSummary = snapshot.normalizedReactionSummary(),
                    currentUserReaction = snapshot.normalizedCurrentUserReaction(accountId),
                    replyCount = snapshot.replyCount,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = snapshot.likeList?.size,
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = cachedAt,
                ),
            ),
        )
        trimRetainedFeed(accountId)
        markFeedOnline(accountId, cachedAt)
    }

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int {
        setSnapshotReaction(
            snapshot = snapshot,
            emoji = if (isChecked) Constants.DEFAULT_HEART_REACTION else null,
        )
        return 1
    }

    override suspend fun setSnapshotReaction(snapshot: Snapshot, emoji: String?) {
        val accountId = authSessionProvider.requireCurrentUserId()
        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, snapshot.snapshotKey)
        val updatedLocalState = buildUpdatedLocalReactionState(
            snapshot = snapshot,
            cachedSnapshot = cachedSnapshot,
            nextEmoji = emoji,
        )

        cachedSnapshot?.let { existing ->
            offlineFeedItemDao.upsertAll(
                listOf(
                    existing.copy(
                        likeCount = updatedLocalState.reactionCount,
                        likedByCurrentUser = updatedLocalState.currentUserReaction != null,
                        reactionCount = updatedLocalState.reactionCount,
                        reactionSummary = updatedLocalState.reactionSummary,
                        currentUserReaction = updatedLocalState.currentUserReaction,
                        hasPendingReaction = true,
                    ),
                ),
            )
        }

        pendingSnapshotActionDao.upsert(
            PendingSnapshotActionEntity(
                actionId = "reaction-$accountId-${snapshot.snapshotKey}",
                accountId = accountId,
                snapshotId = snapshot.snapshotKey,
                actionType = if (updatedLocalState.currentUserReaction == null) {
                    PendingSnapshotActionType.REMOVE_REACTION
                } else {
                    PendingSnapshotActionType.SET_REACTION
                },
                payload = SnapshotInteractionSyncCoordinator.encodeReactionPayload(updatedLocalState.currentUserReaction),
                createdAt = System.currentTimeMillis(),
                lastAttemptAt = null,
                attemptCount = 0,
                queueState = PendingSnapshotActionQueueState.QUEUED,
                supersedesActionId = null,
            ),
        )
        interactionSyncCoordinator.refreshPendingFlags(accountId, snapshot.snapshotKey)
        interactionSyncCoordinator.flushPendingActions(
            accountId = accountId,
            rollbackOnFailure = false,
        )
        interactionSyncCoordinator.refreshPendingFlags(accountId, snapshot.snapshotKey)
    }

    override suspend fun getSnapshotReplies(snapshot: Snapshot): Result<List<SnapshotReply>> {
        val accountId = authSessionProvider.currentUserIdOrNull() ?: return Result.success(emptyList())
        val pendingReplies = pendingSnapshotActionDao.getBySnapshot(accountId, snapshot.snapshotKey)
            .filter {
                it.actionType == PendingSnapshotActionType.ADD_REPLY &&
                    (it.queueState == PendingSnapshotActionQueueState.QUEUED ||
                        it.queueState == PendingSnapshotActionQueueState.IN_FLIGHT)
            }
            .map { action ->
                SnapshotInteractionSyncCoordinator.decodeReplyPayload(snapshot.snapshotKey, action.payload)
            }

        return runCatching {
            remoteDatabaseService.getSnapshotReplies(snapshot.snapshotKey)
        }.map { remoteReplies ->
            (remoteReplies + pendingReplies)
                .sortedWith(compareBy<SnapshotReply> { it.dateTime }.thenBy { it.replyId })
        }.recoverCatching { failure ->
            if (pendingReplies.isNotEmpty()) {
                pendingReplies.sortedWith(compareBy<SnapshotReply> { it.dateTime }.thenBy { it.replyId })
            } else {
                throw failure
            }
        }
    }

    override suspend fun addSnapshotReply(snapshot: Snapshot, text: String): Result<Unit> = runCatching {
        val accountId = authSessionProvider.requireCurrentUserId()
        val currentUser = authSessionProvider.currentUserSnapshotOrNull()
            ?: throw IllegalStateException("No signed-in user")
        val trimmedText = text.trim()
        require(trimmedText.isNotBlank()) { "blank" }
        require(trimmedText.length <= Constants.REPLY_CHAR_LIMIT) { "length" }
        val resolvedUserName = currentUser.displayName
            .ifBlank {
                runCatching { remoteDatabaseService.getUserNameOnce(currentUser.userId) }
                    .getOrDefault("")
            }
            .ifBlank { "Unknown user" }
        val resolvedUserPhotoUrl = currentUser.photoUrl
            .takeIf(String::isNotBlank)
            ?: runCatching {
                remoteDatabaseService.getUserPhotoUrlOnce(currentUser.userId)
            }.getOrDefault("").takeIf(String::isNotBlank)

        val localReply = SnapshotReply(
            replyId = "local-$accountId-${System.currentTimeMillis()}",
            snapshotId = snapshot.snapshotKey,
            idUserOwner = currentUser.userId,
            userName = resolvedUserName,
            userPhotoUrl = resolvedUserPhotoUrl,
            text = trimmedText,
            dateTime = System.currentTimeMillis(),
            deliveryState = SnapshotReplyDeliveryState.PENDING,
        )
        pendingSnapshotActionDao.upsert(
            PendingSnapshotActionEntity(
                actionId = "reply-${localReply.replyId}",
                accountId = accountId,
                snapshotId = snapshot.snapshotKey,
                actionType = PendingSnapshotActionType.ADD_REPLY,
                payload = SnapshotInteractionSyncCoordinator.encodeReplyPayload(localReply),
                createdAt = localReply.dateTime,
                lastAttemptAt = null,
                attemptCount = 0,
                queueState = PendingSnapshotActionQueueState.QUEUED,
                supersedesActionId = null,
            ),
        )

        offlineFeedItemDao.getBySnapshotId(accountId, snapshot.snapshotKey)?.let { cached ->
            offlineFeedItemDao.upsertAll(
                listOf(
                    cached.copy(
                        replyCount = cached.replyCount + 1,
                        hasPendingReply = true,
                    ),
                ),
            )
        }
        interactionSyncCoordinator.refreshPendingFlags(accountId, snapshot.snapshotKey)
        interactionSyncCoordinator.flushPendingActions(
            accountId = accountId,
            rollbackOnFailure = false,
        )
        interactionSyncCoordinator.refreshPendingFlags(accountId, snapshot.snapshotKey)
    }

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int {
        val accountId = authSessionProvider.currentUserIdOrNull()
            ?: return remoteDatabaseService.deleteSnapshot(snapshot)
        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, snapshot.snapshotKey)
        val result = remoteDatabaseService.deleteSnapshot(snapshot)
        if (result != 0) {
            offlineFeedItemDao.deleteBySnapshotId(accountId, snapshot.snapshotKey)
            cleanupOrphanedAssets(
                accountId = accountId,
                assetIds = listOfNotNull(
                    cachedSnapshot?.mainImageAssetId,
                    cachedSnapshot?.ownerAvatarAssetId,
                ),
            )
            markFeedOnline(accountId, System.currentTimeMillis())
        }
        return result
    }

    private suspend fun trimRetainedFeed(accountId: String) {
        val cachedItems = offlineFeedItemDao.getByAccount(accountId)
        if (cachedItems.size <= Constants.OFFLINE_FEED_RETENTION_LIMIT) return

        val overflowItems = cachedItems.drop(Constants.OFFLINE_FEED_RETENTION_LIMIT)
        offlineFeedItemDao.deleteBySnapshotIds(
            accountId = accountId,
            snapshotIds = overflowItems.map(OfflineFeedItemEntity::snapshotId),
        )
        cleanupOrphanedAssets(
            accountId = accountId,
            assetIds = overflowItems.flatMap { item ->
                listOfNotNull(item.mainImageAssetId, item.ownerAvatarAssetId)
            },
        )
    }

    private suspend fun cleanupOrphanedAssets(accountId: String, assetIds: List<String>) {
        val uniqueAssetIds = assetIds.distinct()
        if (uniqueAssetIds.isEmpty()) return

        val orphanedAssets = offlineMediaAssetDao.getByIds(uniqueAssetIds).filter { asset ->
            offlineFeedItemDao.countAssetReferences(accountId, asset.assetId) == 0
        }
        if (orphanedAssets.isEmpty()) return

        offlineMediaAssetDao.deleteByIds(orphanedAssets.map(OfflineMediaAssetEntity::assetId))
        feedAssetStorage.deleteFiles(orphanedAssets.mapNotNull(OfflineMediaAssetEntity::localPath))
    }

    private suspend fun resolveCurrentUserAvatar(
        accountId: String,
        ownerUserId: String,
        snapshotPhotoUrl: String?,
        authPhotoUrl: String,
    ): ResolvedAvatar {
        val retainedAvatarAsset = ownerUserId
            .takeIf(String::isNotBlank)
            ?.let { userId ->
                offlineMediaAssetDao.getByIds(listOf(buildAvatarAssetId(accountId, userId))).firstOrNull()
            }
        val backendPhotoUrl = ownerUserId
            .takeIf(String::isNotBlank)
            ?.let { userId ->
                runCatching { remoteDatabaseService.getUserPhotoUrlOnce(userId) }
                    .getOrDefault("")
            }
            .orEmpty()
        val resolvedRemoteUrl = listOf(
            backendPhotoUrl,
            retainedAvatarAsset?.sourceUrl.orEmpty(),
            snapshotPhotoUrl.orEmpty(),
            authPhotoUrl,
        ).firstOrNull(String::isNotBlank).orEmpty()

        return ResolvedAvatar(
            remoteUrl = resolvedRemoteUrl,
            assetId = retainedAvatarAsset?.assetId,
        )
    }

    private suspend fun markFeedOnline(accountId: String, timestamp: Long) {
        val previousState = feedSyncStateDao.get(accountId)
        val retainedItemCount = offlineFeedItemDao.countByAccount(accountId)
        feedSyncStateDao.upsert(
            FeedSyncStateEntity(
                accountId = accountId,
                lastSuccessfulSyncAt = timestamp,
                lastRefreshAttemptAt = timestamp,
                lastRefreshResult = HomeFeedLastRefreshResult.SUCCESS,
                retainedItemCount = retainedItemCount,
                retentionLimit = previousState?.retentionLimit ?: Constants.OFFLINE_FEED_RETENTION_LIMIT,
                hasRetainedContent = retainedItemCount > 0,
            ),
        )
    }

    private fun buildUpdatedLocalReactionState(
        snapshot: Snapshot,
        cachedSnapshot: OfflineFeedItemEntity?,
        nextEmoji: String?,
    ): LocalReactionState {
        val currentReaction = cachedSnapshot?.currentUserReaction
            ?: snapshot.normalizedCurrentUserReaction(authSessionProvider.currentUserIdOrNull())
        val currentSummary = cachedSnapshot?.reactionSummary
            ?: snapshot.normalizedReactionSummary()
        val sanitizedNextEmoji = nextEmoji?.takeIf(String::isNotBlank)
        val resolvedNextEmoji = when {
            sanitizedNextEmoji == null -> null
            sanitizedNextEmoji == currentReaction -> null
            else -> sanitizedNextEmoji
        }

        val updatedSummary = currentSummary.toMutableMap().apply {
            currentReaction?.let { emoji ->
                val currentCount = (this[emoji] ?: 0) - 1
                if (currentCount > 0) {
                    this[emoji] = currentCount
                } else {
                    remove(emoji)
                }
            }
            resolvedNextEmoji?.let { emoji ->
                this[emoji] = (this[emoji] ?: 0) + 1
            }
        }
        return LocalReactionState(
            currentUserReaction = resolvedNextEmoji,
            reactionSummary = updatedSummary,
            reactionCount = updatedSummary.values.sum(),
        )
    }

    private fun buildAvatarAssetId(accountId: String, ownerUserId: String): String =
        "avatar-$accountId-$ownerUserId"

    private data class LocalReactionState(
        val currentUserReaction: String?,
        val reactionSummary: Map<String, Int>,
        val reactionCount: Int,
    )

    private data class ResolvedAvatar(
        val remoteUrl: String,
        val assetId: String?,
    )
}
