package com.hvasoft.dailydose.presentation.screens.common

import android.content.res.ColorStateList
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
            params.bottomMargin + extraBottomMargin
        )
        snackBar.view.layoutParams = params
        snackBar.show()
    }
}

fun ImageView.loadImage(
    url: String?,
    placeholderResId: Int = R.drawable.image_placeholder,
    errorResId: Int = R.drawable.image_error,
    isCircle: Boolean = false
) {
    var glideRequest = Glide.with(this)
        .load(url)
        .placeholder(placeholderResId)
        .error(errorResId)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
    if (isCircle) glideRequest = glideRequest.circleCrop()
    glideRequest.into(this)
}

fun CheckBox.getLikeCountText(): String {
    val count = text.toString().toInt()
    return if (isChecked) (count + 1).toString() else (count - 1).toString()
}

fun CheckBox.setDynamicTint(uncheckedColor: Int, checkedColor: Int) {
    this.buttonTintList = ColorStateList.valueOf(if (isChecked) checkedColor else uncheckedColor)
    this.setOnCheckedChangeListener { _, isChecked ->
        this.buttonTintList =
            ColorStateList.valueOf(if (isChecked) checkedColor else uncheckedColor)
    }
}

fun TextView.getPostTimeLabel(dateTime: Long?) {
    if (dateTime != null) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - dateTime
        val seconds = timeDiff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = weeks / 4
        val years = months / 12

        text = when {
            years > 0 -> context.resources.getQuantityString(
                R.plurals.years_date_time_label_text,
                years.toInt(), years.toInt()
            )

            months > 0 -> context.resources.getQuantityString(
                R.plurals.months_date_time_label_text,
                months.toInt(), months.toInt()
            )

            weeks > 0 -> context.resources.getQuantityString(
                R.plurals.weeks_date_time_label_text,
                weeks.toInt(), weeks.toInt()
            )

            days > 0 -> {
                if (days > 1)
                    context.resources.getQuantityString(
                        R.plurals.days_date_time_label_text,
                        days.toInt(), days.toInt()
                    )
                else
                    context.getString(R.string.yesterday_date_time_label_text)
            }

            hours > 0 -> context.getString(R.string.hours_date_time_label_text, hours)
            minutes > 5 -> context.resources.getQuantityString(
                R.plurals.minutes_date_time_label_text,
                minutes.toInt(), minutes.toInt()
            )

            else -> context.getString(R.string.moment_date_time_label_text)
        }
    } else text = ""
}
