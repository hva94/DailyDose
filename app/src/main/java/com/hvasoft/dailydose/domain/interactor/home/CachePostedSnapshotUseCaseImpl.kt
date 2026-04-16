package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import javax.inject.Inject

class CachePostedSnapshotUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository,
) : CachePostedSnapshotUseCase {

    override suspend fun invoke(snapshot: Snapshot) {
        homeRepository.cachePostedSnapshot(snapshot)
    }
}
