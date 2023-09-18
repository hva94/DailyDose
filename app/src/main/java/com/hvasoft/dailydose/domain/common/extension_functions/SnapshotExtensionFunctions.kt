package com.hvasoft.dailydose.domain.common.extension_functions

import com.hvasoft.dailydose.domain.model.Snapshot

fun Snapshot.getLikeCountText(): String {
    return if (this.likeList?.keys?.size != null)
        this.likeList.keys.size.toString() else "0"
}

fun Snapshot.isLikeChecked(): Boolean {
    return this.likeList?.containsKey(this.idUserOwner) ?: false
}