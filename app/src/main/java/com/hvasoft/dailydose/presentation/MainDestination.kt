package com.hvasoft.dailydose.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hvasoft.dailydose.R

enum class MainDestination(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME(
        labelRes = R.string.home_title,
        iconRes = R.drawable.ic_pool,
    ),
    ADD(
        labelRes = R.string.home_add,
        iconRes = R.drawable.ic_dropper,
    ),
    PROFILE(
        labelRes = R.string.home_profile,
        iconRes = R.drawable.ic_person,
    ),
}
