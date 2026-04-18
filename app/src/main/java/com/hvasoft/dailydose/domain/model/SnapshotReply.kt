package com.hvasoft.dailydose.domain.model

enum class SnapshotReplyDeliveryState {
    CONFIRMED,
    PENDING,
}

data class SnapshotReply(
    val replyId: String,
    val snapshotId: String,
    val idUserOwner: String,
    val userName: String,
    val userPhotoUrl: String?,
    val text: String,
    val dateTime: Long,
    val deliveryState: SnapshotReplyDeliveryState = SnapshotReplyDeliveryState.CONFIRMED,
)
