package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.common.extension_functions.canRevealImage
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import javax.inject.Inject

class RevealSnapshotUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository,
) : RevealSnapshotUseCase {

    override suspend fun invoke(snapshot: Snapshot) {
        if (!snapshot.canRevealImage()) return
        homeRepository.revealSnapshot(snapshot)
    }
}
