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
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val remoteDatabaseService: com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService,
    private val offlineFeedItemDao: OfflineFeedItemDao,
    private val offlineMediaAssetDao: OfflineMediaAssetDao,
    private val feedSyncStateDao: FeedSyncStateDao,
    private val offlineFeedMapper: OfflineFeedMapper,
    private val refreshCoordinator: HomeFeedRefreshCoordinator,
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
        return refreshCoordinator.refresh(accountId)
    }

    override suspend fun clearOfflineSnapshots(accountId: String) {
        refreshCoordinator.clearAccount(accountId)
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
                    availabilityStatus = OfflineItemAvailabilityStatus.MEDIA_PARTIAL,
                    syncedAt = cachedAt,
                ),
            ),
        )
        trimRetainedFeed(accountId)
        markFeedOnline(accountId, cachedAt)
    }

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int {
        val accountId = authSessionProvider.requireCurrentUserId()
        val cachedSnapshot = offlineFeedItemDao.getBySnapshotId(accountId, snapshot.snapshotKey)
        val previousLikeCount = cachedSnapshot?.likeCount ?: snapshot.likeCount.toIntOrNull() ?: 0
        val previousLiked = cachedSnapshot?.likedByCurrentUser ?: snapshot.isLikedByCurrentUser
        val updatedLikeCount = when {
            previousLiked == isChecked -> previousLikeCount
            isChecked -> previousLikeCount + 1
            else -> (previousLikeCount - 1).coerceAtLeast(0)
        }

        cachedSnapshot?.let {
            offlineFeedItemDao.updateLikeState(
                accountId = accountId,
                snapshotId = snapshot.snapshotKey,
                likeCount = updatedLikeCount,
                likedByCurrentUser = isChecked,
            )
        }

        return try {
            val result = remoteDatabaseService.toggleUserLike(snapshot, isChecked)
            if (result == 0) {
                revertLikeState(accountId, snapshot.snapshotKey, previousLikeCount, previousLiked, cachedSnapshot)
            } else {
                markFeedOnline(accountId, System.currentTimeMillis())
            }
            result
        } catch (exception: Exception) {
            revertLikeState(accountId, snapshot.snapshotKey, previousLikeCount, previousLiked, cachedSnapshot)
            throw exception
        }
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

    private suspend fun revertLikeState(
        accountId: String,
        snapshotId: String,
        previousLikeCount: Int,
        previousLiked: Boolean,
        cachedSnapshot: OfflineFeedItemEntity?,
    ) {
        if (cachedSnapshot == null) return
        offlineFeedItemDao.updateLikeState(
            accountId = accountId,
            snapshotId = snapshotId,
            likeCount = previousLikeCount,
            likedByCurrentUser = previousLiked,
        )
    }

    private fun buildAvatarAssetId(accountId: String, ownerUserId: String): String =
        "avatar-$accountId-$ownerUserId"

    private data class ResolvedAvatar(
        val remoteUrl: String,
        val assetId: String?,
    )
}
