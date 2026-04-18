package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot

interface SetSnapshotReactionUseCase {
    suspend operator fun invoke(snapshot: Snapshot, emoji: String?)
}
