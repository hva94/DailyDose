package com.hvasoft.dailydose.domain.interactor.add

import com.hvasoft.dailydose.domain.model.CreateSnapshotResult
import com.hvasoft.dailydose.domain.repository.AddSnapshotRepository
import javax.inject.Inject

class CreateSnapshotUseCaseImpl @Inject constructor(
    private val addSnapshotRepository: AddSnapshotRepository,
) : CreateSnapshotUseCase {

    override suspend fun invoke(
        title: String,
        localImageContentUri: String,
        onProgress: (Int) -> Unit,
    ): CreateSnapshotResult =
        addSnapshotRepository.publishSnapshot(localImageContentUri, title, onProgress)
}
