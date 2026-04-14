package com.hvasoft.dailydose.domain.interactor.profile

import com.hvasoft.dailydose.domain.model.UserProfile
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import javax.inject.Inject

class GetUserProfileUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
) : GetUserProfileUseCase {

    override suspend fun invoke(userId: String): Result<UserProfile?> =
        profileRepository.loadUserProfile(userId)
}
