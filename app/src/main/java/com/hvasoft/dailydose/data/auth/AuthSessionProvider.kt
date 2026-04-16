package com.hvasoft.dailydose.data.auth

import com.google.firebase.auth.FirebaseUser

interface AuthSessionProvider {
    fun currentUserOrNull(): FirebaseUser?
    fun currentUserIdOrNull(): String?
    fun requireCurrentUserId(): String
    fun currentUserSnapshotOrNull(): CurrentUserSnapshot?
}
