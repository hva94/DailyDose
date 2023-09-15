package com.hvasoft.dailydose.domain.common.error_handling

import com.hvasoft.dailydose.R

object ErrorHandler {

    fun handleError(error: Error): ErrorState {
        return when (error) {
            Error.Connectivity -> ErrorState(
                errorMessageRes = R.string.error_connectivity,
                isTryAgainBtnDisplayed = true
            )

            is Error.CustomException -> ErrorState(
                errorMessageRes = error.messageResId
            )

            is Error.Unknown -> ErrorState(
                errorMessageRes = R.string.error_unknown
            )
        }
    }

}