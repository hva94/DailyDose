package com.hvasoft.dailydose.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfflineFeedItemDao {

    @Query(
        """
        SELECT feed.accountId,
               feed.snapshotId,
               feed.ownerUserId,
               feed.title,
               feed.publishedAt,
               feed.sortOrder,
               feed.remotePhotoUrl,
               feed.mainImageAssetId,
               feed.ownerDisplayName,
               feed.ownerAvatarRemoteUrl,
               feed.ownerAvatarAssetId,
               feed.likeCount,
               feed.likedByCurrentUser,
               feed.availabilityStatus,
               feed.syncedAt,
               main.localPath AS mainLocalPath,
               main.downloadStatus AS mainDownloadStatus,
               avatar.localPath AS avatarLocalPath,
               avatar.downloadStatus AS avatarDownloadStatus
          FROM offline_feed_items AS feed
          LEFT JOIN offline_media_assets AS main
            ON feed.mainImageAssetId = main.assetId
          LEFT JOIN offline_media_assets AS avatar
            ON feed.ownerAvatarAssetId = avatar.assetId
         WHERE feed.accountId = :accountId
         ORDER BY feed.sortOrder ASC
        """
    )
    fun pagingSource(accountId: String): PagingSource<Int, OfflineFeedItemWithAssets>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<OfflineFeedItemEntity>)

    @Query("DELETE FROM offline_feed_items WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Query("SELECT COUNT(*) FROM offline_feed_items WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: String): Int
}
