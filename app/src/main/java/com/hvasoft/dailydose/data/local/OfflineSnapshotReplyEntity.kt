package com.hvasoft.dailydose.data.local

import androidx.room.Entity
import androidx.room.Index
import com.hvasoft.dailydose.domain.model.SnapshotReply
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState

@Entity(
    tableName = "offline_snapshot_replies",
    primaryKeys = ["accountId", "snapshotId", "replyId"],
    indices = [
        Index(value = ["accountId", "snapshotId"]),
    ],
)
data class OfflineSnapshotReplyEntity(
    val accountId: String,
    val snapshotId: String,
    val replyId: String,
    val ownerUserId: String,
    val userName: String,
    val userPhotoUrl: String?,
    val text: String,
    val dateTime: Long,
    val deliveryState: SnapshotReplyDeliveryState,
)

fun OfflineSnapshotReplyEntity.toDomain(): SnapshotReply = SnapshotReply(
    replyId = replyId,
    snapshotId = snapshotId,
    idUserOwner = ownerUserId,
    userName = userName,
    userPhotoUrl = userPhotoUrl,
    text = text,
    dateTime = dateTime,
    deliveryState = deliveryState,
)

fun SnapshotReply.toOfflineEntity(accountId: String): OfflineSnapshotReplyEntity = OfflineSnapshotReplyEntity(
    accountId = accountId,
    snapshotId = snapshotId,
    replyId = replyId,
    ownerUserId = idUserOwner,
    userName = userName,
    userPhotoUrl = userPhotoUrl,
    text = text,
    dateTime = dateTime,
    deliveryState = deliveryState,
)
