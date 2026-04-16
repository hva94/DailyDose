package com.hvasoft.dailydose.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthSessionProvider @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthSessionProvider {

    override fun currentUserOrNull(): FirebaseUser? = firebaseAuth.currentUser

    override fun currentUserIdOrNull(): String? = currentUserOrNull()?.uid

    override fun requireCurrentUserId(): String =
        currentUserIdOrNull() ?: throw IllegalStateException("No signed-in user")

    override fun currentUserSnapshotOrNull(): CurrentUserSnapshot? = currentUserOrNull()?.let { user ->
        CurrentUserSnapshot(
            userId = user.uid,
            displayName = user.displayName.orEmpty(),
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString().orEmpty(),
        )
    }
}
