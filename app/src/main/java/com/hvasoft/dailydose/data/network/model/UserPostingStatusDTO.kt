package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserPostingStatusDTO(
    val lastPostedAt: Long? = null,
    val lastPromptComboId: String? = null,
)
