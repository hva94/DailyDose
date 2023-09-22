package com.hvasoft.dailydose.domain.common.extension_functions

import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.domain.model.Snapshot

fun Snapshot.getLikeCountText(): String {
    if (likeList == null) return "0"
    val isLikedByCurrentUser = likeList.containsKey(this.idUserOwner)
    val likeCountText = likeList.keys.size
    return if (likeCountText == 0 && isLikedByCurrentUser)
        (likeCountText + 1).toString()
    else
        likeCountText.toString()
}

fun Snapshot.isLikedByCurrentUser(): Boolean {
    if (likeList == null) return false
    return likeList.containsKey(Constants.currentUser.uid)
}

fun Snapshot.isCurrentUserOwner(): Boolean {
    if (this.idUserOwner == null) return false
    return this.idUserOwner == Constants.currentUser.uid
}
