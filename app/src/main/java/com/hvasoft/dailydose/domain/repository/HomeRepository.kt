package com.hvasoft.dailydose.domain.repository

import androidx.paging.PagingData
import com.hvasoft.dailydose.domain.model.Snapshot
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    suspend fun getPagedSnapshots(): Flow<PagingData<Snapshot>>
//    suspend fun isLikeChanged(snapshot: Snapshot, isLiked: Boolean): Boolean
//    suspend fun isSnapshotDeleted(snapshot: Snapshot): Boolean

}