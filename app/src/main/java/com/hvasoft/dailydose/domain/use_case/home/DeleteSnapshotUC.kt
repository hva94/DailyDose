package com.hvasoft.dailydose.domain.use_case.home

import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.domain.HomeRepository
import com.hvasoft.dailydose.domain.common.response_handling.Result
import com.hvasoft.dailydose.domain.common.response_handling.fold
import javax.inject.Inject

class DeleteSnapshotUC @Inject constructor(
    private val repository: HomeRepository
) {

    suspend operator fun invoke(snapshot: Snapshot): Result<List<Snapshot>> {
        return try {
            val isSnapshotDeleted = repository.isSnapshotDeleted(snapshot)
            if (isSnapshotDeleted) {
                repository.getSnapshots().fold(
                    onSuccess = {
                        Result.Success(it)
                    },
                    onError = { code, _ ->
                        Result.Error(code)
                    },
                    onException = {
                        Result.Exception(it)
                    }
                )
            } else {
                Result.Error(R.string.home_delete_photo_error)
            }
        } catch (e: Exception) {
            Result.Exception(e)
        }
    }
}