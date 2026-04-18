package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.repository.HomeRepository
import javax.inject.Inject

class GetSnapshotRepliesUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository,
) : GetSnapshotRepliesUseCase {

    override suspend fun invoke(snapshot: Snapshot): Result<List<SnapshotReply>> =
        homeRepository.getSnapshotReplies(snapshot)
}
