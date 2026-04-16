package com.hvasoft.dailydose.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileLocalCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(userId: String): CachedProfile? {
        val displayName = prefs.getString(displayNameKey(userId), null)
        val photoUrl = prefs.getString(photoUrlKey(userId), null)
        val localPhotoPath = prefs.getString(localPhotoPathKey(userId), null)
        val email = prefs.getString(emailKey(userId), null)
        if (
            displayName.isNullOrBlank() &&
            photoUrl.isNullOrBlank() &&
            localPhotoPath.isNullOrBlank() &&
            email.isNullOrBlank()
        ) {
            return null
        }
        return CachedProfile(
            displayName = displayName.orEmpty(),
            photoUrl = photoUrl.orEmpty(),
            localPhotoPath = localPhotoPath.orEmpty(),
            email = email.orEmpty(),
        )
    }

    fun save(
        userId: String,
        displayName: String,
        photoUrl: String,
        localPhotoPath: String = "",
        email: String = "",
    ) {
        prefs.edit()
            .putString(displayNameKey(userId), displayName)
            .putString(photoUrlKey(userId), photoUrl)
            .putString(localPhotoPathKey(userId), localPhotoPath)
            .putString(emailKey(userId), email)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "profile_local_cache"

        fun displayNameKey(userId: String): String = "display_name_$userId"
        fun photoUrlKey(userId: String): String = "photo_url_$userId"
        fun localPhotoPathKey(userId: String): String = "local_photo_path_$userId"
        fun emailKey(userId: String): String = "email_$userId"
    }
}

data class CachedProfile(
    val displayName: String,
    val photoUrl: String,
    val localPhotoPath: String,
    val email: String,
)
