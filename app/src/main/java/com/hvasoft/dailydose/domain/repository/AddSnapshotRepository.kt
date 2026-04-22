package com.hvasoft.dailydose.domain.repository

import com.hvasoft.dailydose.domain.model.CreateSnapshotRequest
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult

interface AddSnapshotRepository {

    suspend fun publishSnapshot(
        request: CreateSnapshotRequest,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult
}
