package com.hvasoft.dailydose.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "offline_feed_items",
    primaryKeys = ["accountId", "snapshotId"],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["mainImageAssetId"]),
        Index(value = ["ownerAvatarAssetId"]),
    ],
)
data class OfflineFeedItemEntity(
    val accountId: String,
    val snapshotId: String,
    val ownerUserId: String,
    val title: String,
    val dailyPromptId: String? = null,
    val dailyPromptText: String? = null,
    val publishedAt: Long,
    val sortOrder: Long,
    val remotePhotoUrl: String,
    val mainImageAssetId: String?,
    val ownerDisplayName: String,
    val ownerAvatarRemoteUrl: String,
    val ownerAvatarAssetId: String?,
    val likeCount: Int,
    val likedByCurrentUser: Boolean,
    val reactionCount: Int = 0,
    val reactionSummary: Map<String, Int> = emptyMap(),
    val currentUserReaction: String? = null,
    val replyCount: Int = 0,
    val hasPendingReaction: Boolean = false,
    val hasPendingReply: Boolean = false,
    val legacyLikeCount: Int? = null,
    val availabilityStatus: OfflineItemAvailabilityStatus,
    val syncedAt: Long,
)

enum class OfflineItemAvailabilityStatus {
    FULLY_AVAILABLE,
    MEDIA_PARTIAL,
}

data class OfflineFeedItemWithAssets(
    @ColumnInfo(name = "accountId") val accountId: String,
    @ColumnInfo(name = "snapshotId") val snapshotId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "dailyPromptId") val dailyPromptId: String? = null,
    @ColumnInfo(name = "dailyPromptText") val dailyPromptText: String? = null,
    @ColumnInfo(name = "publishedAt") val publishedAt: Long,
    @ColumnInfo(name = "sortOrder") val sortOrder: Long,
    @ColumnInfo(name = "remotePhotoUrl") val remotePhotoUrl: String,
    @ColumnInfo(name = "mainImageAssetId") val mainImageAssetId: String?,
    @ColumnInfo(name = "ownerDisplayName") val ownerDisplayName: String,
    @ColumnInfo(name = "ownerAvatarRemoteUrl") val ownerAvatarRemoteUrl: String,
    @ColumnInfo(name = "ownerAvatarAssetId") val ownerAvatarAssetId: String?,
    @ColumnInfo(name = "likeCount") val likeCount: Int,
    @ColumnInfo(name = "likedByCurrentUser") val likedByCurrentUser: Boolean,
    @ColumnInfo(name = "reactionCount") val reactionCount: Int = 0,
    @ColumnInfo(name = "reactionSummary") val reactionSummary: Map<String, Int> = emptyMap(),
    @ColumnInfo(name = "currentUserReaction") val currentUserReaction: String? = null,
    @ColumnInfo(name = "replyCount") val replyCount: Int = 0,
    @ColumnInfo(name = "hasPendingReaction") val hasPendingReaction: Boolean = false,
    @ColumnInfo(name = "hasPendingReply") val hasPendingReply: Boolean = false,
    @ColumnInfo(name = "legacyLikeCount") val legacyLikeCount: Int? = null,
    @ColumnInfo(name = "availabilityStatus") val availabilityStatus: OfflineItemAvailabilityStatus,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long,
    @ColumnInfo(name = "mainLocalPath") val mainLocalPath: String?,
    @ColumnInfo(name = "mainDownloadStatus") val mainDownloadStatus: OfflineMediaDownloadStatus?,
    @ColumnInfo(name = "avatarLocalPath") val avatarLocalPath: String?,
    @ColumnInfo(name = "avatarDownloadStatus") val avatarDownloadStatus: OfflineMediaDownloadStatus?,
)
