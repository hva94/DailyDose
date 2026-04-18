package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot

interface AddSnapshotReplyUseCase {
    suspend operator fun invoke(snapshot: Snapshot, text: String): Result<Unit>
}
