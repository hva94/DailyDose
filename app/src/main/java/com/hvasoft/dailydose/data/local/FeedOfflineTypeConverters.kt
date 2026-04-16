package com.hvasoft.dailydose.data.local

import androidx.room.TypeConverter
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult

class FeedOfflineTypeConverters {

    @TypeConverter
    fun fromOfflineItemAvailabilityStatus(value: OfflineItemAvailabilityStatus?): String? = value?.name

    @TypeConverter
    fun toOfflineItemAvailabilityStatus(value: String?): OfflineItemAvailabilityStatus? =
        value?.let(OfflineItemAvailabilityStatus::valueOf)

    @TypeConverter
    fun fromOfflineMediaAssetType(value: OfflineMediaAssetType?): String? = value?.name

    @TypeConverter
    fun toOfflineMediaAssetType(value: String?): OfflineMediaAssetType? =
        value?.let(OfflineMediaAssetType::valueOf)

    @TypeConverter
    fun fromOfflineMediaDownloadStatus(value: OfflineMediaDownloadStatus?): String? = value?.name

    @TypeConverter
    fun toOfflineMediaDownloadStatus(value: String?): OfflineMediaDownloadStatus? =
        value?.let(OfflineMediaDownloadStatus::valueOf)

    @TypeConverter
    fun fromHomeFeedLastRefreshResult(value: HomeFeedLastRefreshResult?): String? = value?.name

    @TypeConverter
    fun toHomeFeedLastRefreshResult(value: String?): HomeFeedLastRefreshResult? =
        value?.let(HomeFeedLastRefreshResult::valueOf)
}
