package com.hvasoft.dailydose.data.common

import com.google.firebase.auth.FirebaseUser

object Constants {

    lateinit var currentUser: FirebaseUser

    const val SNAPSHOTS_PATH = "snapshots"
    const val USERS_PATH = "users"
    const val USERNAME_PATH = "userName"
    const val PHOTO_URL_PATH = "photoUrl"
    const val LIKE_LIST_PROPERTY = "likeList"
    const val SNAPSHOTS_ITEMS_PER_PAGE = 20
    const val INDEX_ONE = 1

}