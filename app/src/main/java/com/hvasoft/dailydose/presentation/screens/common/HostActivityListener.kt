package com.hvasoft.dailydose.presentation.screens.common

import com.google.android.material.snackbar.Snackbar

interface HostActivityListener {
    fun showPopUpMessage(
        resId: Int,
        duration: Int = Snackbar.LENGTH_SHORT
    )
    fun onSnapshotPosted()
}