package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.DailyPromptAssignment
import kotlinx.coroutines.flow.Flow

interface GetActiveDailyPromptUseCase {
    operator fun invoke(): Flow<DailyPromptAssignment?>
}
