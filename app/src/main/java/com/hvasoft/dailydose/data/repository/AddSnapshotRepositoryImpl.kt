package com.hvasoft.dailydose.data.repository

import com.hvasoft.dailydose.data.network.data_source.RemoteDatabaseService
import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.repository.AddSnapshotRepository
import javax.inject.Inject

class AddSnapshotRepositoryImpl @Inject constructor(
    private val remoteDatabaseService: RemoteDatabaseService,
) : AddSnapshotRepository {

    override suspend fun publishSnapshot(
        localImageContentUri: String,
        title: String,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult =
        remoteDatabaseService.publishSnapshot(localImageContentUri, title, onProgress)
}
