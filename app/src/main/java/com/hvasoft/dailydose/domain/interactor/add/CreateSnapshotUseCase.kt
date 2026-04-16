package com.hvasoft.dailydose.domain.interactor.add

import com.hvasoft.dailydose.domain.model.CreateSnapshotResult

interface CreateSnapshotUseCase {

    suspend operator fun invoke(
        title: String,
        localImageContentUri: String,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult
}
