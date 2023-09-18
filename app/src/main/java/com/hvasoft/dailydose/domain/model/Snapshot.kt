package com.hvasoft.dailydose.domain.model

data class Snapshot(
    val snapshotId: Int = 0,
    val title: String? = null,
    val dateTime: Long? = null,
    val photoUrl: String? = null,
    val likeList: Map<String, Boolean>? = null,
    val idUserOwner: String? = null,
    var userName: String? = null,
    var userPhotoUrl: String? = null
)