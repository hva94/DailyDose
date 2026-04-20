package com.hvasoft.dailydose.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState

@Dao
interface OfflineSnapshotReplyDao {

    @Query(
        """
        SELECT * FROM offline_snapshot_replies
         WHERE accountId = :accountId
           AND snapshotId = :snapshotId
         ORDER BY dateTime ASC, replyId ASC
        """
    )
    suspend fun getBySnapshot(accountId: String, snapshotId: String): List<OfflineSnapshotReplyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<OfflineSnapshotReplyEntity>)

    @Query(
        """
        DELETE FROM offline_snapshot_replies
         WHERE accountId = :accountId
           AND snapshotId = :snapshotId
           AND deliveryState = :deliveryState
        """
    )
    suspend fun deleteBySnapshotAndDeliveryState(
        accountId: String,
        snapshotId: String,
        deliveryState: SnapshotReplyDeliveryState,
    )

    @Query(
        """
        DELETE FROM offline_snapshot_replies
         WHERE accountId = :accountId
           AND snapshotId = :snapshotId
           AND replyId = :replyId
        """
    )
    suspend fun deleteByReplyId(accountId: String, snapshotId: String, replyId: String)

    @Query(
        """
        DELETE FROM offline_snapshot_replies
         WHERE accountId = :accountId
           AND snapshotId = :snapshotId
        """
    )
    suspend fun deleteBySnapshot(accountId: String, snapshotId: String)

    @Query("DELETE FROM offline_snapshot_replies WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
