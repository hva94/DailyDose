package com.hvasoft.dailydose.domain.model

sealed interface CreateSnapshotResult {
    data class Success(val snapshot: Snapshot) : CreateSnapshotResult
    data object ImageUploadFailed : CreateSnapshotResult
    data object SaveFailed : CreateSnapshotResult
}
