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
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.domain.model.PostSnapshotOutcome
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeOfflineFeedItemDao : OfflineFeedItemDao {
    private val storedItems = mutableListOf<OfflineFeedItemEntity>()
    private val projections = mutableMapOf<String, List<OfflineFeedItemWithAssets>>()

    override fun pagingSource(accountId: String): PagingSource<Int, OfflineFeedItemWithAssets> =
        StaticPagingSource(projections[accountId].orEmpty())

    override suspend fun upsertAll(items: List<OfflineFeedItemEntity>) {
        val accountId = items.firstOrNull()?.accountId ?: return
        storedItems.removeAll { it.accountId == accountId }
        storedItems += items
    }

    override suspend fun deleteByAccount(accountId: String) {
        storedItems.removeAll { it.accountId == accountId }
        projections.remove(accountId)
    }

    override suspend fun countByAccount(accountId: String): Int =
        storedItems.count { it.accountId == accountId }

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

    override suspend fun deleteByIds(assetIds: List<String>) {
        storedAssets.removeAll { it.assetId in assetIds }
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

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int = 1

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int = 1

    override suspend fun publishSnapshot(
        localImageContentUri: String,
        title: String,
        onProgress: (Int) -> Unit,
    ): PostSnapshotOutcome = PostSnapshotOutcome.SUCCESS
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
