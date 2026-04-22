package com.hvasoft.dailydose.domain.model

data class UserPostingStatus(
    val userId: String,
    val lastPostedAt: Long? = null,
    val lastPromptComboId: String? = null,
) {
    fun hasPostedToday(dateKey: String = DailyPromptDay.currentDateKey()): Boolean =
        DailyPromptDay.isSamePromptDay(lastPostedAt, dateKey)
}
