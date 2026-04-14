package com.hvasoft.dailydose.domain.model

data class UserProfile(
    val userId: String,
    val displayName: String,
    val photoUrl: String,
    val email: String,
)
