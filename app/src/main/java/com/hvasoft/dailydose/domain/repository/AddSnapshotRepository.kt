package com.hvasoft.dailydose.domain.repository

import com.hvasoft.dailydose.domain.model.PostSnapshotOutcome

interface AddSnapshotRepository {

    suspend fun publishSnapshot(
        localImageContentUri: String,
        title: String,
        onProgress: (Int) -> Unit,
    ): PostSnapshotOutcome
}
