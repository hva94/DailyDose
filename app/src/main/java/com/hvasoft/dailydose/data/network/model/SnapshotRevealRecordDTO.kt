package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SnapshotRevealRecordDTO(
    val revealedAt: Long = 0L,
)
