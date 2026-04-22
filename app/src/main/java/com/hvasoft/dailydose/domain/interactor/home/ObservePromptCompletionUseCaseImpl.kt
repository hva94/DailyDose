package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObservePromptCompletionUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository,
) : ObservePromptCompletionUseCase {
    override fun invoke(): Flow<Boolean> =
        homeRepository.observeUserPostingStatus()
            .map { postingStatus -> postingStatus?.hasPostedToday() == true }
}
