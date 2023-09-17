package com.hvasoft.dailydose.presentation.screens.utils

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.hvasoft.dailydose.R

fun Fragment.showPopUpMessage(msg: Any, isError: Boolean = false) {
    val duration = if (isError) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
    val message = if (msg is Int) getString(msg) else msg.toString()
    view?.let { rootView ->
        val snackBar = Snackbar.make(rootView, message, duration)
        val params = snackBar.view.layoutParams as ViewGroup.MarginLayoutParams
        val extraBottomMargin = resources.getDimensionPixelSize(R.dimen.common_padding_default)
        params.setMargins(
            params.leftMargin,
            params.topMargin,
            params.rightMargin,
//                binding.floatingButton.height + params.bottomMargin + extraBottomMargin
            params.bottomMargin + extraBottomMargin
        )
        snackBar.view.layoutParams = params
        snackBar.show()
    }
}