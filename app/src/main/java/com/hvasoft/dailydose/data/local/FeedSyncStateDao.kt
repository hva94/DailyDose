package com.hvasoft.dailydose.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedSyncStateDao {

    @Query("SELECT * FROM feed_sync_state WHERE accountId = :accountId LIMIT 1")
    fun observe(accountId: String): Flow<FeedSyncStateEntity?>

    @Query("SELECT * FROM feed_sync_state WHERE accountId = :accountId LIMIT 1")
    suspend fun get(accountId: String): FeedSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: FeedSyncStateEntity)

    @Query("DELETE FROM feed_sync_state WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
