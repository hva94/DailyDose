package com.example.snapshots

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SnapshotUser(@get:Exclude var id: String = "",
                        var userName: String = "",
                        var photoUrl: String = "")
