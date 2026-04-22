package com.hvasoft.dailydose.domain.interactor.home

import kotlinx.coroutines.flow.Flow

interface ObservePromptCompletionUseCase {
    operator fun invoke(): Flow<Boolean>
}
