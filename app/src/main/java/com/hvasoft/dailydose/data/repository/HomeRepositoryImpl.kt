package com.hvasoft.dailydose.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.data.paging.SnapshotPagingSource
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val remoteDatabaseService: RemoteDatabaseService
) : HomeRepository {

    override suspend fun getPagedSnapshots(): Flow<PagingData<Snapshot>> {
        val pagingSource = SnapshotPagingSource(remoteDatabaseService)
        return Pager(
            config = PagingConfig(pageSize = Constants.SNAPSHOTS_ITEMS_PER_PAGE),
            pagingSourceFactory = { pagingSource }
        ).flow
    }

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int {
        return remoteDatabaseService.toggleUserLike(snapshot, isChecked)
    }

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int {
        return remoteDatabaseService.deleteSnapshot(snapshot)
    }

}