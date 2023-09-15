package com.hvasoft.dailydose.domain.use_case.home

import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.domain.HomeRepository
import com.hvasoft.dailydose.domain.common.response_handling.Result
import javax.inject.Inject

class IsLikeChangedUC @Inject constructor(
    private val repository: HomeRepository
) {

    suspend operator fun invoke(snapshot: Snapshot, isLiked: Boolean): Result<Boolean> {
        return try {
            val isLikeChanged = repository.isLikeChanged(snapshot, isLiked)
            if (isLikeChanged) {
                Result.Success(true)
            } else {
                Result.Error(R.string.home_update_photo_error)
            }
        } catch (e: Exception) {
            Result.Exception(e)
        }
    }

}