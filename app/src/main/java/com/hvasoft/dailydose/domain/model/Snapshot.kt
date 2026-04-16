package com.hvasoft.dailydose.domain.model

import java.io.File

data class Snapshot(
    val title: String? = null,
    val dateTime: Long? = null,
    val photoUrl: String? = null,
    val likeList: Map<String, Boolean>? = null,
    val idUserOwner: String? = null,

    val paginationId: Int = 0,
    var snapshotKey: String = "",
    var userName: String? = null,
    var userPhotoUrl: String? = null,
    var isLikedByCurrentUser: Boolean = false,
    var likeCount: String = "0",
    val localPhotoPath: String? = null,
    val localUserPhotoPath: String? = null,
    val isOfflineMediaPartial: Boolean = false,
    val syncedAt: Long? = null,
) {
    fun preferredPhotoModel(allowRemoteFallback: Boolean = true): Any? =
        localPhotoFileOrNull() ?: if (allowRemoteFallback) photoUrl else null

    fun preferredUserPhotoModel(allowRemoteFallback: Boolean = true): Any? =
        localUserPhotoFileOrNull() ?: if (allowRemoteFallback) userPhotoUrl else null

    fun hasRetainedMainImage(): Boolean = localPhotoFileOrNull() != null

    fun hasAnyImageAvailable(allowRemoteFallback: Boolean = true): Boolean =
        preferredPhotoModel(allowRemoteFallback) != null

    private fun localPhotoFileOrNull(): File? = localPhotoPath
        ?.let(::File)
        ?.takeIf(File::exists)

    private fun localUserPhotoFileOrNull(): File? = localUserPhotoPath
        ?.let(::File)
        ?.takeIf(File::exists)
}
