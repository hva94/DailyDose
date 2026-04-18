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
               feed.reactionCount,
               feed.reactionSummary,
               feed.currentUserReaction,
               feed.replyCount,
               feed.hasPendingReaction,
               feed.hasPendingReply,
               feed.legacyLikeCount,
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

    @Query("SELECT * FROM offline_feed_items WHERE accountId = :accountId ORDER BY sortOrder ASC")
    suspend fun getByAccount(accountId: String): List<OfflineFeedItemEntity>

    @Query(
        """
        SELECT * FROM offline_feed_items
         WHERE accountId = :accountId AND snapshotId = :snapshotId
         LIMIT 1
        """
    )
    suspend fun getBySnapshotId(accountId: String, snapshotId: String): OfflineFeedItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<OfflineFeedItemEntity>)

    @Query("UPDATE offline_feed_items SET sortOrder = sortOrder + 1 WHERE accountId = :accountId")
    suspend fun incrementSortOrders(accountId: String)

    @Query(
        """
        UPDATE offline_feed_items
           SET likeCount = :likeCount,
               likedByCurrentUser = :likedByCurrentUser
         WHERE accountId = :accountId AND snapshotId = :snapshotId
        """
    )
    suspend fun updateLikeState(
        accountId: String,
        snapshotId: String,
        likeCount: Int,
        likedByCurrentUser: Boolean,
    )

    @Query("DELETE FROM offline_feed_items WHERE accountId = :accountId AND snapshotId = :snapshotId")
    suspend fun deleteBySnapshotId(accountId: String, snapshotId: String)

    @Query("DELETE FROM offline_feed_items WHERE accountId = :accountId AND snapshotId IN (:snapshotIds)")
    suspend fun deleteBySnapshotIds(accountId: String, snapshotIds: List<String>)

    @Query(
        """
        DELETE FROM offline_feed_items
         WHERE accountId = :accountId AND snapshotId NOT IN (:snapshotIds)
        """
    )
    suspend fun deleteMissingSnapshots(accountId: String, snapshotIds: List<String>)

    @Query("DELETE FROM offline_feed_items WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Query("SELECT COUNT(*) FROM offline_feed_items WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM offline_feed_items
         WHERE accountId = :accountId
           AND (mainImageAssetId = :assetId OR ownerAvatarAssetId = :assetId)
        """
    )
    suspend fun countAssetReferences(accountId: String, assetId: String): Int

    @Query(
        """
        SELECT ownerDisplayName,
               ownerAvatarRemoteUrl,
               ownerAvatarAssetId,
               avatar.localPath AS ownerAvatarLocalPath
          FROM offline_feed_items AS feed
          LEFT JOIN offline_media_assets AS avatar
            ON feed.ownerAvatarAssetId = avatar.assetId
         WHERE feed.accountId = :accountId
           AND feed.ownerUserId = :ownerUserId
         ORDER BY feed.syncedAt DESC, feed.sortOrder ASC
         LIMIT 1
        """
    )
    suspend fun getLatestOwnerProfile(accountId: String, ownerUserId: String): OfflineOwnerProfileCache?

    @Query(
        """
        SELECT avatar.localPath
          FROM offline_feed_items AS feed
          JOIN offline_media_assets AS avatar
            ON feed.ownerAvatarAssetId = avatar.assetId
         WHERE feed.accountId = :accountId
           AND feed.ownerUserId = :ownerUserId
           AND avatar.localPath IS NOT NULL
         ORDER BY feed.syncedAt DESC, feed.sortOrder ASC
         LIMIT 1
        """
    )
    suspend fun getLatestOwnerAvatarLocalPath(accountId: String, ownerUserId: String): String?
}

data class OfflineOwnerProfileCache(
    val ownerDisplayName: String,
    val ownerAvatarRemoteUrl: String,
    val ownerAvatarAssetId: String?,
    val ownerAvatarLocalPath: String?,
)
