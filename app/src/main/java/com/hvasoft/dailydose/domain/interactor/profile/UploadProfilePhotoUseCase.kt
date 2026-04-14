package com.hvasoft.dailydose.domain.interactor.profile

/** Returns the new public photo URL on success. */
interface UploadProfilePhotoUseCase {
    suspend operator fun invoke(
        userId: String,
        localImageContentUri: String,
        currentUserName: String,
        onProgress: (Int) -> Unit,
    ): Result<String>
}
