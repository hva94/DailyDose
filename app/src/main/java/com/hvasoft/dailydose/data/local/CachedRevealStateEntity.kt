package com.hvasoft.dailydose.data.local

import androidx.room.Entity
import androidx.room.Index
import com.hvasoft.dailydose.domain.model.SnapshotRevealSyncState

@Entity(
    tableName = "cached_reveal_states",
    primaryKeys = ["accountId", "snapshotId"],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "snapshotId"]),
    ],
)
data class CachedRevealStateEntity(
    val accountId: String,
    val snapshotId: String,
    val revealedAt: Long,
    val syncState: SnapshotRevealSyncState = SnapshotRevealSyncState.CONFIRMED,
    val updatedAt: Long,
)
