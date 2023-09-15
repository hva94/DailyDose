package com.hvasoft.dailydose.domain.use_case.home

import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.domain.HomeRepository
import com.hvasoft.dailydose.domain.common.response_handling.Result
import com.hvasoft.dailydose.domain.common.response_handling.fold
import javax.inject.Inject

class GetSnapshotsUC @Inject constructor(
    private val repository: HomeRepository
) {

    suspend operator fun invoke(): Result<List<Snapshot>> {
        return try {
            repository.getSnapshots().fold(
                onSuccess = {
                    Result.Success(it)
                },
                onError = { code, _ ->
                    Result.Error(code)
                }
            ) {
                Result.Exception(it)
            }
        } catch (e: Exception) {
            Result.Exception(e)
        }
    }

}