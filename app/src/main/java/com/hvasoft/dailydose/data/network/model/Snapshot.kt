package com.hvasoft.dailydose.data.network.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.sql.Timestamp

@IgnoreExtraProperties
data class Snapshot(
    @get:Exclude var id: String = "",
    var title: String = "",
    var dateTime: Long = Timestamp(System.currentTimeMillis()).time,
    var photoUrl: String = "",
    var likeList: Map<String, Boolean> = mutableMapOf(),
    var idUserOwner: String = "",
    @get:Exclude var userName: String = "",
    @get:Exclude var userPhotoUrl: String = ""
)
