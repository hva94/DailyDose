package com.hvasoft.dailydose.data.network.model

import com.hvasoft.dailydose.domain.model.Snapshot

data class SnapshotsMainResponse(
    var snapshots: List<Snapshot>? = null,
    var exception: Exception? = null
)