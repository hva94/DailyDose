package com.hvasoft.dailydose.presentation.utils

import com.google.android.material.snackbar.Snackbar

interface MainAux {
    fun showMessage(
        resId: Int,
        duration: Int = Snackbar.LENGTH_SHORT
    )
}