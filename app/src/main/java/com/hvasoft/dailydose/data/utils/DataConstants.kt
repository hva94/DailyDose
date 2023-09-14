package com.hvasoft.dailydose.data.utils

import com.google.firebase.auth.FirebaseUser

object DataConstants {
    const val SNAPSHOTS_PATH = "snapshots"
    const val USERS_PATH = "users"
    const val LIKE_LIST_PROPERTY = "likeList"

    lateinit var currentUser: FirebaseUser
}