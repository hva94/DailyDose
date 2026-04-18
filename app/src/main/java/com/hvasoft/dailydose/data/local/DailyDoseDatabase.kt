package com.hvasoft.dailydose.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        OfflineFeedItemEntity::class,
        OfflineMediaAssetEntity::class,
        FeedSyncStateEntity::class,
        PendingSnapshotActionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(FeedOfflineTypeConverters::class)
abstract class DailyDoseDatabase : RoomDatabase() {
    abstract fun offlineFeedItemDao(): OfflineFeedItemDao
    abstract fun offlineMediaAssetDao(): OfflineMediaAssetDao
    abstract fun feedSyncStateDao(): FeedSyncStateDao
    abstract fun pendingSnapshotActionDao(): PendingSnapshotActionDao
}
