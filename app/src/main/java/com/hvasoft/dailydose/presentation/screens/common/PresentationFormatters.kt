package com.hvasoft.dailydose.presentation.screens.common

import android.content.res.Resources
import com.hvasoft.dailydose.R

fun formatRelativeTime(resources: Resources, dateTime: Long?): String {
    if (dateTime == null) return ""

    val currentTime = System.currentTimeMillis()
    val timeDiff = currentTime - dateTime
    val seconds = timeDiff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = weeks / 4
    val years = months / 12

    return when {
        years > 0 -> resources.getQuantityString(
            R.plurals.years_date_time_label_text,
            years.toInt(),
            years.toInt(),
        )

        months > 0 -> resources.getQuantityString(
            R.plurals.months_date_time_label_text,
            months.toInt(),
            months.toInt(),
        )

        weeks > 0 -> resources.getQuantityString(
            R.plurals.weeks_date_time_label_text,
            weeks.toInt(),
            weeks.toInt(),
        )

        days > 0 -> {
            if (days > 1) {
                resources.getQuantityString(
                    R.plurals.days_date_time_label_text,
                    days.toInt(),
                    days.toInt(),
                )
            } else {
                resources.getString(R.string.yesterday_date_time_label_text)
            }
        }

        hours > 0 -> resources.getString(R.string.hours_date_time_label_text, hours)
        minutes > 5 -> resources.getQuantityString(
            R.plurals.minutes_date_time_label_text,
            minutes.toInt(),
            minutes.toInt(),
        )

        else -> resources.getString(R.string.moment_date_time_label_text)
    }
}
