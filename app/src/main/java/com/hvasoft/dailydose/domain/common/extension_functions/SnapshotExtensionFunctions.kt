package com.hvasoft.dailydose.domain.common.extension_functions

import com.hvasoft.dailydose.domain.model.Snapshot

fun Snapshot.getLikeCountText(): String {
    if (likeList == null) return "0"
    return likeList.keys.size.toString()
}

fun Snapshot.isLikedBy(userId: String?): Boolean {
    if (likeList == null) return false
    val currentUserId = userId ?: return false
    return likeList.containsKey(currentUserId)
}

fun Snapshot.isOwnedBy(userId: String?): Boolean {
    if (this.idUserOwner == null) return false
    val currentUserId = userId ?: return false
    return this.idUserOwner == currentUserId
}
