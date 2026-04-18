package com.hvasoft.dailydose.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.FeedSyncStateEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedItemEntity
import com.hvasoft.dailydose.data.local.OfflineFeedItemWithAssets
import com.hvasoft.dailydose.data.local.OfflineMediaAssetDao
import com.hvasoft.dailydose.data.local.OfflineMediaAssetEntity
import com.hvasoft.dailydose.data.local.OfflineOwnerProfileCache
import com.hvasoft.dailydose.data.local.PendingSnapshotActionDao
import com.hvasoft.dailydose.data.local.PendingSnapshotActionEntity
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.data.network.model.User
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionQueueState
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeOfflineFeedItemDao : OfflineFeedItemDao {
    private val storedItems = mutableListOf<OfflineFeedItemEntity>()
    private val projections = mutableMapOf<String, List<OfflineFeedItemWithAssets>>()

    override fun pagingSource(accountId: String): PagingSource<Int, OfflineFeedItemWithAssets> =
        StaticPagingSource(projections[accountId].orEmpty())

    override suspend fun getByAccount(accountId: String): List<OfflineFeedItemEntity> =
        storedItems.filter { it.accountId == accountId }.sortedBy { it.sortOrder }

    override suspend fun getBySnapshotId(accountId: String, snapshotId: String): OfflineFeedItemEntity? =
        storedItems.firstOrNull { it.accountId == accountId && it.snapshotId == snapshotId }

    override suspend fun upsertAll(items: List<OfflineFeedItemEntity>) {
        items.forEach { newItem ->
            storedItems.removeAll {
                it.accountId == newItem.accountId && it.snapshotId == newItem.snapshotId
            }
            storedItems += newItem
        }
    }

    override suspend fun incrementSortOrders(accountId: String) {
        val updatedItems = storedItems.map { item ->
            if (item.accountId == accountId) item.copy(sortOrder = item.sortOrder + 1) else item
        }
        storedItems.clear()
        storedItems += updatedItems
    }

    override suspend fun updateLikeState(
        accountId: String,
        snapshotId: String,
        likeCount: Int,
        likedByCurrentUser: Boolean,
    ) {
        val updatedItems = storedItems.map { item ->
            if (item.accountId == accountId && item.snapshotId == snapshotId) {
                item.copy(
                    likeCount = likeCount,
                    likedByCurrentUser = likedByCurrentUser,
                    reactionCount = likeCount,
                    reactionSummary = emptyMap(),
                    currentUserReaction = null,
                    replyCount = 0,
                    hasPendingReaction = false,
                    hasPendingReply = false,
                    legacyLikeCount = null,
                )
            } else {
                item
            }
        }
        storedItems.clear()
        storedItems += updatedItems
    }

    override suspend fun deleteBySnapshotId(accountId: String, snapshotId: String) {
        storedItems.removeAll { it.accountId == accountId && it.snapshotId == snapshotId }
    }

    override suspend fun deleteBySnapshotIds(accountId: String, snapshotIds: List<String>) {
        storedItems.removeAll { it.accountId == accountId && it.snapshotId in snapshotIds }
    }

    override suspend fun deleteMissingSnapshots(accountId: String, snapshotIds: List<String>) {
        storedItems.removeAll { it.accountId == accountId && it.snapshotId !in snapshotIds }
    }

    override suspend fun deleteByAccount(accountId: String) {
        storedItems.removeAll { it.accountId == accountId }
        projections.remove(accountId)
    }

    override suspend fun countByAccount(accountId: String): Int =
        storedItems.count { it.accountId == accountId }

    override suspend fun countAssetReferences(accountId: String, assetId: String): Int =
        storedItems.count { item ->
            item.accountId == accountId &&
                (item.mainImageAssetId == assetId || item.ownerAvatarAssetId == assetId)
        }

    override suspend fun getLatestOwnerProfile(
        accountId: String,
        ownerUserId: String,
    ): OfflineOwnerProfileCache? = storedItems
        .filter { it.accountId == accountId && it.ownerUserId == ownerUserId }
        .sortedWith(compareByDescending<OfflineFeedItemEntity> { it.syncedAt }.thenBy { it.sortOrder })
        .firstOrNull()
        ?.let { item ->
            OfflineOwnerProfileCache(
                ownerDisplayName = item.ownerDisplayName,
                ownerAvatarRemoteUrl = item.ownerAvatarRemoteUrl,
                ownerAvatarAssetId = item.ownerAvatarAssetId,
                ownerAvatarLocalPath = null,
            )
        }

    override suspend fun getLatestOwnerAvatarLocalPath(
        accountId: String,
        ownerUserId: String,
    ): String? = null

    fun setPagingItems(accountId: String, items: List<OfflineFeedItemWithAssets>) {
        projections[accountId] = items
    }

    fun storedItems(accountId: String): List<OfflineFeedItemEntity> =
        storedItems.filter { it.accountId == accountId }
}

internal class FakeOfflineMediaAssetDao : OfflineMediaAssetDao {
    private val storedAssets = mutableListOf<OfflineMediaAssetEntity>()

    override suspend fun upsertAll(items: List<OfflineMediaAssetEntity>) {
        items.forEach { newAsset ->
            storedAssets.removeAll { it.assetId == newAsset.assetId }
            storedAssets += newAsset
        }
    }

    override suspend fun getByAccount(accountId: String): List<OfflineMediaAssetEntity> =
        storedAssets.filter { it.accountId == accountId }

    override suspend fun getByIds(assetIds: List<String>): List<OfflineMediaAssetEntity> =
        storedAssets.filter { it.assetId in assetIds }

    override suspend fun deleteByIds(assetIds: List<String>) {
        storedAssets.removeAll { it.assetId in assetIds }
    }

    override suspend fun deleteById(assetId: String) {
        storedAssets.removeAll { it.assetId == assetId }
    }

    override suspend fun deleteMissingAssets(accountId: String, assetIds: List<String>) {
        storedAssets.removeAll { it.accountId == accountId && it.assetId !in assetIds }
    }

    override suspend fun deleteByAccount(accountId: String) {
        storedAssets.removeAll { it.accountId == accountId }
    }

    fun storedAssets(accountId: String): List<OfflineMediaAssetEntity> =
        storedAssets.filter { it.accountId == accountId }

}

internal class FakeFeedSyncStateDao : FeedSyncStateDao {
    private val states = mutableMapOf<String, MutableStateFlow<FeedSyncStateEntity?>>()

    override fun observe(accountId: String): Flow<FeedSyncStateEntity?> =
        states.getOrPut(accountId) { MutableStateFlow(null) }

    override suspend fun get(accountId: String): FeedSyncStateEntity? =
        states[accountId]?.value

    override suspend fun upsert(state: FeedSyncStateEntity) {
        states.getOrPut(state.accountId) { MutableStateFlow(null) }.value = state
    }

    override suspend fun deleteByAccount(accountId: String) {
        states.remove(accountId)
    }

    fun setState(state: FeedSyncStateEntity) {
        states.getOrPut(state.accountId) { MutableStateFlow(null) }.value = state
    }

    fun currentState(accountId: String): FeedSyncStateEntity? = states[accountId]?.value
}

internal class FakeRemoteDatabaseService(
    private val snapshots: List<Snapshot> = emptyList(),
    private val namesByUserId: Map<String, String> = emptyMap(),
    private val avatarsByUserId: Map<String, String> = emptyMap(),
) : RemoteDatabaseService {
    override fun getSnapshots(): Flow<List<Snapshot>> = flowOf(snapshots)

    override suspend fun getSnapshotsOnce(): List<Snapshot> = snapshots

    override fun getUserPhotoUrl(idUser: String?): Flow<String> = flowOf(avatarsByUserId[idUser].orEmpty())

    override suspend fun getUserPhotoUrlOnce(idUser: String?): String = avatarsByUserId[idUser].orEmpty()

    override fun getUserName(idUser: String?): Flow<String> = flowOf(namesByUserId[idUser].orEmpty())

    override suspend fun getUserNameOnce(idUser: String?): String = namesByUserId[idUser].orEmpty()

    override suspend fun getUsersOnce(userIds: Set<String>): Map<String, User> = userIds.associateWith { userId ->
        User(
            id = userId,
            userName = namesByUserId[userId].orEmpty(),
            photoUrl = avatarsByUserId[userId].orEmpty(),
        )
    }

    override suspend fun setSnapshotReaction(snapshotId: String, emoji: String?): Int = 1

    override suspend fun getSnapshotReplies(snapshotId: String): List<SnapshotReply> = emptyList()

    override suspend fun addSnapshotReply(snapshotId: String, reply: SnapshotReply): SnapshotReply = reply

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int = 1

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int = 1

    override suspend fun publishSnapshot(
        localImageContentUri: String,
        title: String,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult = CreateSnapshotResult.Success(
        Snapshot(
            title = title,
            snapshotKey = "snapshot-created",
        ),
    )
}

internal class FakePendingSnapshotActionDao : PendingSnapshotActionDao {
    private val storedActions = mutableListOf<PendingSnapshotActionEntity>()

    override suspend fun upsert(action: PendingSnapshotActionEntity) {
        storedActions.removeAll { it.actionId == action.actionId }
        storedActions += action
    }

    override suspend fun upsertAll(actions: List<PendingSnapshotActionEntity>) {
        actions.forEach { action ->
            upsert(action)
        }
    }

    override suspend fun getByAccount(accountId: String): List<PendingSnapshotActionEntity> =
        storedActions.filter { it.accountId == accountId }.sortedBy(PendingSnapshotActionEntity::createdAt)

    override suspend fun getBySnapshot(accountId: String, snapshotId: String): List<PendingSnapshotActionEntity> =
        storedActions.filter { it.accountId == accountId && it.snapshotId == snapshotId }
            .sortedBy(PendingSnapshotActionEntity::createdAt)

    override suspend fun updateState(
        actionId: String,
        queueState: PendingSnapshotActionQueueState,
        lastAttemptAt: Long?,
        attemptCount: Int,
    ) {
        val updated = storedActions.map { action ->
            if (action.actionId == actionId) {
                action.copy(
                    queueState = queueState,
                    lastAttemptAt = lastAttemptAt,
                    attemptCount = attemptCount,
                )
            } else {
                action
            }
        }
        storedActions.clear()
        storedActions += updated
    }

    override suspend fun deleteById(actionId: String) {
        storedActions.removeAll { it.actionId == actionId }
    }

    override suspend fun deleteByAccount(accountId: String) {
        storedActions.removeAll { it.accountId == accountId }
    }
}

private class StaticPagingSource<T : Any>(
    private val items: List<T>,
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = LoadResult.Page(
        data = items,
        prevKey = null,
        nextKey = null,
    )
}
