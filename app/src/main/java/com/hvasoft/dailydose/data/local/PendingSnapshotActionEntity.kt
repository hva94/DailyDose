package com.hvasoft.dailydose.data.local

import androidx.room.Entity
import androidx.room.Index
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionQueueState
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionType

@Entity(
    tableName = "pending_snapshot_actions",
    primaryKeys = ["actionId"],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "snapshotId"]),
    ],
)
data class PendingSnapshotActionEntity(
    val actionId: String,
    val accountId: String,
    val snapshotId: String,
    val actionType: PendingSnapshotActionType,
    val payload: String,
    val createdAt: Long,
    val lastAttemptAt: Long?,
    val attemptCount: Int,
    val queueState: PendingSnapshotActionQueueState,
    val supersedesActionId: String?,
)
