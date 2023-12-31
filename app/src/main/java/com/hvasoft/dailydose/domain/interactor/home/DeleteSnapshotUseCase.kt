package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot

interface DeleteSnapshotUseCase {
    suspend operator fun invoke(snapshot: Snapshot)
}