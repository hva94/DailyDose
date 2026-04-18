package com.hvasoft.dailydose.domain.model

enum class PendingSnapshotActionType {
    SET_REACTION,
    REMOVE_REACTION,
    ADD_REPLY,
}

enum class PendingSnapshotActionQueueState {
    QUEUED,
    IN_FLIGHT,
    FAILED,
    DISCARDED,
}
