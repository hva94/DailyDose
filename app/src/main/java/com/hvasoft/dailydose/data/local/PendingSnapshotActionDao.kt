package com.hvasoft.dailydose.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionQueueState

@Dao
interface PendingSnapshotActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(action: PendingSnapshotActionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(actions: List<PendingSnapshotActionEntity>)

    @Query(
        """
        SELECT * FROM pending_snapshot_actions
         WHERE accountId = :accountId
         ORDER BY createdAt ASC
        """
    )
    suspend fun getByAccount(accountId: String): List<PendingSnapshotActionEntity>

    @Query(
        """
        SELECT * FROM pending_snapshot_actions
         WHERE accountId = :accountId
           AND snapshotId = :snapshotId
         ORDER BY createdAt ASC
        """
    )
    suspend fun getBySnapshot(accountId: String, snapshotId: String): List<PendingSnapshotActionEntity>

    @Query(
        """
        UPDATE pending_snapshot_actions
           SET queueState = :queueState,
               lastAttemptAt = :lastAttemptAt,
               attemptCount = :attemptCount
         WHERE actionId = :actionId
        """
    )
    suspend fun updateState(
        actionId: String,
        queueState: PendingSnapshotActionQueueState,
        lastAttemptAt: Long?,
        attemptCount: Int,
    )

    @Query("DELETE FROM pending_snapshot_actions WHERE actionId = :actionId")
    suspend fun deleteById(actionId: String)

    @Query("DELETE FROM pending_snapshot_actions WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
