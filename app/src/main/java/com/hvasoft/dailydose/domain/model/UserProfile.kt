package com.hvasoft.dailydose.domain.model

import java.io.File

data class UserProfile(
    val userId: String,
    val displayName: String,
    val photoUrl: String,
    val localPhotoPath: String? = null,
    val email: String,
    val isOfflineFallback: Boolean = false,
) {
    fun preferredPhotoModel(authPhotoUrl: String = ""): Any? =
        localPhotoFileOrNull() ?: photoUrl.takeIf(String::isNotBlank) ?: authPhotoUrl.takeIf(String::isNotBlank)

    private fun localPhotoFileOrNull(): File? = localPhotoPath
        ?.let(::File)
        ?.takeIf(File::exists)
}
