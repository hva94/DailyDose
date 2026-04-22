package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveDailyPromptUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository,
) : GetActiveDailyPromptUseCase {
    override fun invoke(): Flow<DailyPromptAssignment?> =
        homeRepository.observeActiveDailyPrompt()
}
