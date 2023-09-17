package com.hvasoft.dailydose.data.network.model

data class SnapshotResponse(
    var snapshots: List<Snapshot>? = null,
    var exception: Exception? = null
)