package com.hvasoft.dailydose.domain.interactor.add

import com.hvasoft.dailydose.domain.model.CreateSnapshotRequest
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult

interface CreateSnapshotUseCase {

    suspend operator fun invoke(
        request: CreateSnapshotRequest,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult
}
