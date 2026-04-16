package com.hvasoft.dailydose.domain.interactor.profile

import com.hvasoft.dailydose.domain.model.UserProfile

/** Returns the refreshed user profile on success. */
interface UploadProfilePhotoUseCase {
    suspend operator fun invoke(
        userId: String,
        localImageContentUri: String,
        currentUserName: String,
        onProgress: (Int) -> Unit,
    ): Result<UserProfile>
}
