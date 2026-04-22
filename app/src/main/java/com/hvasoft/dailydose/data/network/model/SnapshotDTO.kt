package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.hvasoft.dailydose.domain.model.SnapshotTitleGenerationMode
import java.sql.Timestamp

@IgnoreExtraProperties
data class SnapshotDTO(
    val title: String = "",
    val dateTime: Long = Timestamp(System.currentTimeMillis()).time,
    val photoUrl: String = "",
    val likeList: Map<String, Boolean> = mutableMapOf(),
    val idUserOwner: String = "",
    val dailyPromptId: String? = null,
    val dailyPromptText: String? = null,
    val titleGenerationMode: String = SnapshotTitleGenerationMode.NONE.name,
    val reactionCount: Int = 0,
    val reactionSummary: Map<String, Int> = mutableMapOf(),
    val replyCount: Int = 0,

    @get:Exclude val paginationId: Int = 0,
    @get:Exclude val snapshotKey: String = "",
    @get:Exclude val userName: String = "",
    @get:Exclude val userPhotoUrl: String = "",
    @get:Exclude val currentUserReaction: String? = null,
    @get:Exclude val hasPendingReaction: Boolean = false,
    @get:Exclude val hasPendingReply: Boolean = false,
    @get:Exclude val legacyLikeCount: Int? = null,
    @get:Exclude val isLikedByCurrentUser: Boolean = false,
    @get:Exclude val likeCount: String = "0"
)
