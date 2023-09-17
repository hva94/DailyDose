package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    @get:Exclude var id: String = "",
    var userName: String = "",
    var photoUrl: String = ""
)
