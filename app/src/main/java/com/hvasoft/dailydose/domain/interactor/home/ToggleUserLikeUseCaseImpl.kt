package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import javax.inject.Inject

class ToggleUserLikeUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository
) : ToggleUserLikeUseCase {

    override suspend fun invoke(snapshot: Snapshot, isChecked: Boolean) {
        homeRepository.toggleUserLike(snapshot, isChecked)
    }

}