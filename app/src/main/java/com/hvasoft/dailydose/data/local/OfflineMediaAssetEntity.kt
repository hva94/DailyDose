package com.hvasoft.dailydose.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "offline_media_assets",
    indices = [Index(value = ["accountId"])],
)
data class OfflineMediaAssetEntity(
    @PrimaryKey val assetId: String,
    val accountId: String,
    val assetType: OfflineMediaAssetType,
    val sourceUrl: String,
    val localPath: String?,
    val downloadStatus: OfflineMediaDownloadStatus,
    val byteSize: Long,
    val downloadedAt: Long?,
    val lastReferencedAt: Long,
)

enum class OfflineMediaAssetType {
    SNAPSHOT_IMAGE,
    USER_AVATAR,
}

enum class OfflineMediaDownloadStatus {
    READY,
    MISSING,
    FAILED,
}
