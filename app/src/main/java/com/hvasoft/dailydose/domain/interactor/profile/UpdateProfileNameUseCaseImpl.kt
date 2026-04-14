package com.hvasoft.dailydose.domain.interactor.profile

import com.hvasoft.dailydose.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileNameUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
) : UpdateProfileNameUseCase {

    override suspend fun invoke(
        userId: String,
        newName: String,
        fallbackDisplayName: String,
    ): Result<Unit> {
        profileRepository.updateAuthDisplayName(newName).getOrElse { return Result.failure(it) }
        return profileRepository.mergeAndSaveUserRecord(
            userId = userId,
            userName = newName,
            photoUrl = null,
            fallbackDisplayName = fallbackDisplayName,
        )
    }
}
