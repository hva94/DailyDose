package com.hvasoft.dailydose.domain.repository

import com.hvasoft.dailydose.domain.model.UserProfile

interface ProfileRepository {

    suspend fun loadUserProfile(userId: String): Result<UserProfile?>

    suspend fun getCachedAvatarLocalPath(userId: String): String?

    suspend fun uploadProfilePhoto(
        userId: String,
        localImageContentUri: String,
        onProgress: (Int) -> Unit,
    ): Result<String>

    suspend fun mergeAndSaveUserRecord(
        userId: String,
        userName: String?,
        photoUrl: String?,
        fallbackDisplayName: String,
    ): Result<Unit>

    suspend fun updateAuthDisplayName(newName: String): Result<Unit>
}
