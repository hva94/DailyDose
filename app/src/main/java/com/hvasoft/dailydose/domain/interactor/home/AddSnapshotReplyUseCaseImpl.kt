package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import javax.inject.Inject

class AddSnapshotReplyUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository,
) : AddSnapshotReplyUseCase {

    override suspend fun invoke(snapshot: Snapshot, text: String): Result<Unit> =
        homeRepository.addSnapshotReply(snapshot, text)
}
