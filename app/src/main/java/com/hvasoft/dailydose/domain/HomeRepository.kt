package com.hvasoft.dailydose.domain

import com.hvasoft.dailydose.data.network.model.Snapshot
import com.hvasoft.dailydose.domain.common.response_handling.Resource

interface HomeRepository {

//    suspend fun getSnapshots(): Result<List<Snapshot>>
    suspend fun getSnapshots(): Resource<List<Snapshot>>
//    suspend fun isLikeChanged(snapshot: Snapshot, isLiked: Boolean): Boolean
//    suspend fun isSnapshotDeleted(snapshot: Snapshot): Boolean

}