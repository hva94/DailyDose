package com.hvasoft.dailydose.domain.interactor.add

import com.hvasoft.dailydose.domain.model.PostSnapshotOutcome

interface CreateSnapshotUseCase {

    suspend operator fun invoke(
        title: String,
        localImageContentUri: String,
        userId: String,
        onProgress: (Int) -> Unit,
    ): PostSnapshotOutcome
}
