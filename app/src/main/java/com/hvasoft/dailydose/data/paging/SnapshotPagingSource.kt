package com.hvasoft.dailydose.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseDataSource
import com.hvasoft.dailydose.data.utils.Constants.INDEX_ONE
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.firstOrNull

class SnapshotPagingSource(
    private val remoteDatabaseDataSource: RemoteDatabaseDataSource
) : PagingSource<Int, Snapshot>() {

    override fun getRefreshKey(state: PagingState<Int, Snapshot>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(INDEX_ONE)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(INDEX_ONE)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Snapshot> {
        val currentPage = params.key ?: INDEX_ONE
        return try {
            val snapshotsWithoutProfileAndUserNames: List<Snapshot> =
                remoteDatabaseDataSource.getSnapshots().firstOrNull() ?: emptyList()
            val snapshotsWithoutUserNames = snapshotsWithoutProfileAndUserNames.map { snapshot ->
                snapshot.userPhotoUrl =
                    remoteDatabaseDataSource.getUserPhotoUrl(snapshot.idUserOwner).firstOrNull() ?: ""
                snapshot
            }
            val snapshots = snapshotsWithoutUserNames.map { snapshot ->
                snapshot.userName =
                    remoteDatabaseDataSource.getUserName(snapshot.idUserOwner).firstOrNull() ?: ""
                snapshot
            }
            if (snapshots.isNotEmpty()) {
                val endOfPaginationReached = snapshots.isEmpty()
                LoadResult.Page(
                    data = snapshots,
                    prevKey = if (currentPage == INDEX_ONE) null else currentPage - 1,
                    nextKey = if (endOfPaginationReached) null else currentPage + 1
                )
            } else {
                LoadResult.Error(Throwable("TODO - Network problem, the snapshot list is empty - TODO"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
