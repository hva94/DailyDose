package com.hvasoft.dailydose.data.local

import androidx.room.TypeConverter
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionQueueState
import com.hvasoft.dailydose.domain.model.PendingSnapshotActionType
import com.hvasoft.dailydose.domain.model.HomeFeedLastRefreshResult
import com.hvasoft.dailydose.domain.model.SnapshotRevealSyncState
import com.hvasoft.dailydose.domain.model.SnapshotReplyDeliveryState
import com.hvasoft.dailydose.domain.model.SnapshotVisibilityMode
import java.util.Base64

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

    @TypeConverter
    fun fromPendingSnapshotActionType(value: PendingSnapshotActionType?): String? = value?.name

    @TypeConverter
    fun toPendingSnapshotActionType(value: String?): PendingSnapshotActionType? =
        value?.let(PendingSnapshotActionType::valueOf)

    @TypeConverter
    fun fromPendingSnapshotActionQueueState(value: PendingSnapshotActionQueueState?): String? = value?.name

    @TypeConverter
    fun toPendingSnapshotActionQueueState(value: String?): PendingSnapshotActionQueueState? =
        value?.let(PendingSnapshotActionQueueState::valueOf)

    @TypeConverter
    fun fromSnapshotRevealSyncState(value: SnapshotRevealSyncState?): String? = value?.name

    @TypeConverter
    fun toSnapshotRevealSyncState(value: String?): SnapshotRevealSyncState? =
        value?.let(SnapshotRevealSyncState::valueOf)

    @TypeConverter
    fun fromSnapshotVisibilityMode(value: SnapshotVisibilityMode?): String? = value?.name

    @TypeConverter
    fun toSnapshotVisibilityMode(value: String?): SnapshotVisibilityMode? =
        value?.let(SnapshotVisibilityMode::valueOf)

    @TypeConverter
    fun fromSnapshotReplyDeliveryState(value: SnapshotReplyDeliveryState?): String? = value?.name

    @TypeConverter
    fun toSnapshotReplyDeliveryState(value: String?): SnapshotReplyDeliveryState? =
        value?.let(SnapshotReplyDeliveryState::valueOf)

    @TypeConverter
    fun fromReactionSummary(value: Map<String, Int>?): String =
        value.orEmpty()
            .entries
            .sortedBy { it.key }
            .joinToString(separator = REACTION_SUMMARY_ENTRY_DELIMITER.toString()) { (emoji, count) ->
                "${encodeReactionSummaryKey(emoji)}$REACTION_SUMMARY_KEY_VALUE_DELIMITER$count"
            }

    @TypeConverter
    fun toReactionSummary(value: String?): Map<String, Int> {
        if (value.isNullOrBlank()) return emptyMap()
        return buildMap {
            value.split(REACTION_SUMMARY_ENTRY_DELIMITER)
                .filter(String::isNotBlank)
                .forEach { entry ->
                    val separatorIndex = entry.lastIndexOf(REACTION_SUMMARY_KEY_VALUE_DELIMITER)
                    if (separatorIndex <= 0 || separatorIndex == entry.lastIndex) return@forEach

                    val encodedKey = entry.substring(0, separatorIndex)
                    val count = entry.substring(separatorIndex + 1).toIntOrNull() ?: return@forEach
                    put(decodeReactionSummaryKey(encodedKey), count)
                }
        }
    }

    private companion object {
        const val REACTION_SUMMARY_ENTRY_DELIMITER = '|'
        const val REACTION_SUMMARY_KEY_VALUE_DELIMITER = ':'

        fun encodeReactionSummaryKey(value: String): String =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.toByteArray(Charsets.UTF_8))

        fun decodeReactionSummaryKey(value: String): String =
            String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    }
}
