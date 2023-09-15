package com.hvasoft.dailydose.domain.use_case.home

data class HomeUseCases(
    val getSnapshots: GetSnapshotsUC,
    val isLikeChanged: IsLikeChangedUC,
    val deleteSnapshot: DeleteSnapshotUC
)