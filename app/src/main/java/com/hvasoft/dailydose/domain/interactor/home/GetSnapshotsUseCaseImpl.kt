package com.hvasoft.dailydose.domain.interactor.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetSnapshotsUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository
) : GetSnapshotsUseCase {

    @ExperimentalPagingApi
    override fun invoke(): Flow<PagingData<Snapshot>> = flow {
        emitAll(homeRepository.getPagedSnapshots())
    }

}