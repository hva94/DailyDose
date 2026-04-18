package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import javax.inject.Inject

class SetSnapshotReactionUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository,
) : SetSnapshotReactionUseCase {

    override suspend fun invoke(snapshot: Snapshot, emoji: String?) {
        homeRepository.setSnapshotReaction(snapshot, emoji)
    }
}
