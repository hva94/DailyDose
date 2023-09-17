package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.data.network.model.Snapshot
import com.hvasoft.dailydose.domain.common.response_handling.Resource

interface GetSnapshotsUseCase {

    suspend operator fun invoke(): Resource<List<Snapshot>>
}