package com.hvasoft.dailydose.domain.interactor.profile

import com.hvasoft.dailydose.domain.model.UserProfile
import com.hvasoft.dailydose.domain.repository.ProfileRepository
import javax.inject.Inject

class UploadProfilePhotoUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
) : UploadProfilePhotoUseCase {

    override suspend fun invoke(
        userId: String,
        localImageContentUri: String,
        currentUserName: String,
        onProgress: (Int) -> Unit,
    ): Result<UserProfile> {
        val url = profileRepository.uploadProfilePhoto(userId, localImageContentUri, onProgress)
            .getOrElse { return Result.failure(it) }
        profileRepository.mergeAndSaveUserRecord(
            userId = userId,
            userName = currentUserName,
            photoUrl = url,
            fallbackDisplayName = currentUserName,
        ).getOrElse { return Result.failure(it) }
        val updatedProfile = profileRepository.loadUserProfile(userId)
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(IllegalStateException("Updated profile is missing"))
        return Result.success(updatedProfile)
    }
}
