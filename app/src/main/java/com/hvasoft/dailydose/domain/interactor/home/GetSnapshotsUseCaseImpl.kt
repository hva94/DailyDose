package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.data.network.model.Snapshot
import com.hvasoft.dailydose.domain.HomeRepository
import com.hvasoft.dailydose.domain.common.response_handling.Resource
import javax.inject.Inject

class GetSnapshotsUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository
) : GetSnapshotsUseCase {

    override suspend fun invoke(): Resource<List<Snapshot>> = homeRepository.getSnapshots()

}