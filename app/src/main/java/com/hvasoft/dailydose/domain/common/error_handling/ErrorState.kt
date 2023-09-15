package com.hvasoft.dailydose.domain.common.error_handling

import androidx.annotation.StringRes

data class ErrorState(
    val isTryAgainBtnDisplayed: Boolean = false,
    @StringRes val errorMessageRes: Int
)