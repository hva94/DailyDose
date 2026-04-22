package com.hvasoft.dailydose.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedRevealStateDao {

    @Query(
        """
        SELECT * FROM cached_reveal_states
         WHERE accountId = :accountId
        """
    )
    suspend fun getByAccount(accountId: String): List<CachedRevealStateEntity>

    @Query(
        """
        SELECT * FROM cached_reveal_states
         WHERE accountId = :accountId AND snapshotId = :snapshotId
         LIMIT 1
        """
    )
    suspend fun getBySnapshotId(accountId: String, snapshotId: String): CachedRevealStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedRevealStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CachedRevealStateEntity>)

    @Query("DELETE FROM cached_reveal_states WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
