package com.hvasoft.dailydose.presentation.screens.common

import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import com.hvasoft.dailydose.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class PresentationFormattersTest {

    private val resources = mockk<Resources>()

    @Test
    fun formatRelativeTime_returnsMomentAgoForRecentTimes() {
        every { resources.getString(R.string.moment_date_time_label_text) } returns "A moment ago"

        val result = formatRelativeTime(resources, System.currentTimeMillis() - 60_000)

        assertThat(result).isEqualTo("A moment ago")
    }

    @Test
    fun formatRelativeTime_returnsYesterdayForSingleDay() {
        every { resources.getString(R.string.yesterday_date_time_label_text) } returns "Yesterday"

        val result = formatRelativeTime(resources, System.currentTimeMillis() - 86_400_000)

        assertThat(result).isEqualTo("Yesterday")
    }
}
