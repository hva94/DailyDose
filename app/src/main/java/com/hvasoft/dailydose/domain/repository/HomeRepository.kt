package com.hvasoft.dailydose.domain.repository

import androidx.paging.PagingData
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    suspend fun getPagedSnapshots(): Flow<PagingData<Snapshot>>
    fun observeSyncState(): Flow<HomeFeedSyncState>
    suspend fun refreshSnapshots(): Result<Unit>
    suspend fun clearOfflineSnapshots(accountId: String)
    suspend fun cachePostedSnapshot(snapshot: Snapshot)
    suspend fun setSnapshotReaction(snapshot: Snapshot, emoji: String?)
    suspend fun getSnapshotReplies(snapshot: Snapshot): Result<List<SnapshotReply>>
    suspend fun addSnapshotReply(snapshot: Snapshot, text: String): Result<Unit>
    suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int
    suspend fun deleteSnapshot(snapshot: Snapshot): Int

}
