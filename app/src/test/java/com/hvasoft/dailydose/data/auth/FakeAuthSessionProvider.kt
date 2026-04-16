package com.hvasoft.dailydose.data.auth

import com.google.firebase.auth.FirebaseUser

class FakeAuthSessionProvider(
    private var currentUser: FirebaseUser? = null,
) : AuthSessionProvider {

    fun setCurrentUser(user: FirebaseUser?) {
        currentUser = user
    }

    override fun currentUserOrNull(): FirebaseUser? = currentUser

    override fun currentUserIdOrNull(): String? = currentUser?.uid

    override fun requireCurrentUserId(): String =
        currentUserIdOrNull() ?: throw IllegalStateException("No signed-in user")

    override fun currentUserSnapshotOrNull(): CurrentUserSnapshot? = currentUser?.let { user ->
        CurrentUserSnapshot(
            userId = user.uid,
            displayName = user.displayName.orEmpty(),
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString().orEmpty(),
        )
    }
}
