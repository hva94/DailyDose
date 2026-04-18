package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.model.SnapshotReply

interface GetSnapshotRepliesUseCase {
    suspend operator fun invoke(snapshot: Snapshot): Result<List<SnapshotReply>>
}
