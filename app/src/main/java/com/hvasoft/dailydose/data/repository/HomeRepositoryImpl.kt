package com.hvasoft.dailydose.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.hvasoft.dailydose.data.auth.AuthSessionProvider
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.data.local.FeedSyncStateDao
import com.hvasoft.dailydose.data.local.OfflineFeedItemDao
import com.hvasoft.dailydose.data.local.OfflineFeedMapper
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.HomeFeedSyncState
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val remoteDatabaseService: com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService,
    private val offlineFeedItemDao: OfflineFeedItemDao,
    private val feedSyncStateDao: FeedSyncStateDao,
    private val offlineFeedMapper: OfflineFeedMapper,
    private val refreshCoordinator: HomeFeedRefreshCoordinator,
    private val authSessionProvider: AuthSessionProvider,
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

    override suspend fun toggleUserLike(snapshot: Snapshot, isChecked: Boolean): Int {
        return remoteDatabaseService.toggleUserLike(snapshot, isChecked)
    }

    override suspend fun deleteSnapshot(snapshot: Snapshot): Int {
        return remoteDatabaseService.deleteSnapshot(snapshot)
    }

}
