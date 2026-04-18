package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SnapshotReactionDTO(
    val userId: String = "",
    val emoji: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
