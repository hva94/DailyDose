package com.hvasoft.dailydose.domain.model

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
    var likeCount: String = "0"
)