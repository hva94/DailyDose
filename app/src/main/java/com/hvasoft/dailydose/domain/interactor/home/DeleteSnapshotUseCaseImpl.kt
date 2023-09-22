package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import javax.inject.Inject

class DeleteSnapshotUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository
) : DeleteSnapshotUseCase {

    override suspend fun invoke(snapshot: Snapshot) {
        val resultCode = homeRepository.deleteSnapshot(snapshot)
        if (resultCode == 0) throw Exception("Failed to delete snapshot")
    }

}