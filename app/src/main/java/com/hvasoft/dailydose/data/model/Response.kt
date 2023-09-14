package com.hvasoft.dailydose.data.model

data class Response(
    var snapshots: List<Snapshot>? = null,
    var exception: Exception? = null
)