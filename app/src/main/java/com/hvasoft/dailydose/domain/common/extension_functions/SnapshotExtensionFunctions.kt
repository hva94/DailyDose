package com.hvasoft.dailydose.domain.common.extension_functions

import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.domain.model.Snapshot

fun Snapshot.getLikeCountText(): String {
    return normalizedReactionCount().toString()
}

fun Snapshot.isLikedBy(userId: String?): Boolean {
    return normalizedCurrentUserReaction(userId) != null
}

fun Snapshot.isOwnedBy(userId: String?): Boolean {
    if (this.idUserOwner == null) return false
    val currentUserId = userId ?: return false
    return this.idUserOwner == currentUserId
}

fun Snapshot.normalizedReactionSummary(): Map<String, Int> = when {
    reactionSummary.isNotEmpty() -> reactionSummary.filterValues { it > 0 }
    likeList.isNullOrEmpty().not() -> mapOf(Constants.DEFAULT_HEART_REACTION to likeList.orEmpty().size)
    likeCount.toIntOrNull().orZero() > 0 -> mapOf(Constants.DEFAULT_HEART_REACTION to likeCount.toInt())
    else -> emptyMap()
}

fun Snapshot.normalizedReactionCount(): Int = when {
    reactionCount > 0 -> reactionCount
    reactionSummary.isNotEmpty() -> reactionSummary.values.sum()
    likeList.isNullOrEmpty().not() -> likeList.orEmpty().size
    else -> likeCount.toIntOrNull().orZero()
}

fun Snapshot.normalizedCurrentUserReaction(userId: String? = null): String? = when {
    currentUserReaction.isNullOrBlank().not() -> currentUserReaction
    isLikedByCurrentUser -> Constants.DEFAULT_HEART_REACTION
    userId != null && likeList?.containsKey(userId) == true -> Constants.DEFAULT_HEART_REACTION
    else -> null
}

private fun Int?.orZero(): Int = this ?: 0
