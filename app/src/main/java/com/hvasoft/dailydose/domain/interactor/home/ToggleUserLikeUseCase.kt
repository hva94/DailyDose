package com.hvasoft.dailydose.domain.interactor.home

import com.hvasoft.dailydose.domain.model.Snapshot

interface ToggleUserLikeUseCase {
    suspend operator fun invoke(snapshot: Snapshot, isChecked: Boolean)
}