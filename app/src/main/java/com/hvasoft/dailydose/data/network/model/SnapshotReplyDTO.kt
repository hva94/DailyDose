package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SnapshotReplyDTO(
    val idUserOwner: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val text: String = "",
    val dateTime: Long = 0L,
)
