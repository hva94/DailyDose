package com.hvasoft.dailydose.domain.interactor.profile

import com.hvasoft.dailydose.domain.model.UserProfile

interface GetUserProfileUseCase {
    suspend operator fun invoke(userId: String): Result<UserProfile?>
}
