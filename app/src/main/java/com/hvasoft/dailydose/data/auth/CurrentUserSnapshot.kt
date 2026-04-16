package com.hvasoft.dailydose.data.auth

data class CurrentUserSnapshot(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
)
