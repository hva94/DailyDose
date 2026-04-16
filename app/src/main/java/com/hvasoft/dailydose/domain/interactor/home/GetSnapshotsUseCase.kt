package com.hvasoft.dailydose.domain.interactor.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.Flow

interface GetSnapshotsUseCase {

    @ExperimentalPagingApi
    operator fun invoke(): Flow<PagingData<Snapshot>>
    fun observeSyncState(): Flow<HomeFeedSyncState>
    suspend fun refresh(): Result<Unit>
    suspend fun clearOfflineSnapshots(accountId: String)
}
