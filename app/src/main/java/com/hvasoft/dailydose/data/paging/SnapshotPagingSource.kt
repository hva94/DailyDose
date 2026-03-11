package com.hvasoft.dailydose.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hvasoft.dailydose.data.common.Constants.INDEX_ONE
import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.domain.common.extension_functions.getLikeCountText
import com.hvasoft.dailydose.domain.common.extension_functions.isLikedByCurrentUser
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.firstOrNull

class SnapshotPagingSource(
    private val remoteDatabaseService: RemoteDatabaseService
) : PagingSource<Int, Snapshot>() {

    override fun getRefreshKey(state: PagingState<Int, Snapshot>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(INDEX_ONE)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(INDEX_ONE)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Snapshot> {
        return try {
            val snapshots = getSnapshots()
            LoadResult.Page(
                data = snapshots,
                prevKey = null,
                nextKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun getSnapshots(): List<Snapshot> {
        val initialSnapshots = remoteDatabaseService.getSnapshots().firstOrNull() ?: emptyList()
        return initialSnapshots.map { snapshot ->
            val userPhotoUrl =
                remoteDatabaseService.getUserPhotoUrl(snapshot.idUserOwner).firstOrNull() ?: ""
            val userName =
                remoteDatabaseService.getUserName(snapshot.idUserOwner).firstOrNull() ?: ""
            snapshot.apply {
                this.userPhotoUrl = userPhotoUrl
                this.userName = userName
                this.isLikedByCurrentUser = isLikedByCurrentUser()
                this.likeCount = getLikeCountText()
            }
        }
    }
}
