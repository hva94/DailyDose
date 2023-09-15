package com.hvasoft.dailydose.domain

import com.hvasoft.dailydose.domain.common.response_handling.Result
import com.hvasoft.dailydose.data.model.Snapshot

interface HomeRepository {

    suspend fun getSnapshots(): Result<List<Snapshot>>
    suspend fun isLikeChanged(snapshot: Snapshot, isLiked: Boolean): Boolean
    suspend fun isSnapshotDeleted(snapshot: Snapshot): Boolean

}