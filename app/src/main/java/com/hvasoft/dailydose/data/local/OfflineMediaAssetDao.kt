package com.hvasoft.dailydose.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfflineMediaAssetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<OfflineMediaAssetEntity>)

    @Query("SELECT * FROM offline_media_assets WHERE accountId = :accountId")
    suspend fun getByAccount(accountId: String): List<OfflineMediaAssetEntity>

    @Query("DELETE FROM offline_media_assets WHERE assetId IN (:assetIds)")
    suspend fun deleteByIds(assetIds: List<String>)

    @Query("DELETE FROM offline_media_assets WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
