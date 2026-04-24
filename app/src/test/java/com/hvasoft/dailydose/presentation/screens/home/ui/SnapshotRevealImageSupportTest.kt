package com.hvasoft.dailydose.presentation.screens.home.ui

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SnapshotRevealImageSupportTest {

    @Test
    fun `visible snapshot uses the default render mode on any sdk`() {
        assertThat(
            resolveSnapshotRevealImageRenderMode(
                shouldShowHiddenTreatment = false,
                sdkInt = Build.VERSION_CODES.R,
            ),
        ).isEqualTo(SnapshotRevealImageRenderMode.Default)

        assertThat(
            resolveSnapshotRevealImageRenderMode(
                shouldShowHiddenTreatment = false,
                sdkInt = Build.VERSION_CODES.S,
            ),
        ).isEqualTo(SnapshotRevealImageRenderMode.Default)
    }

    @Test
    fun `hidden snapshot uses the legacy transformation on android 11 and lower`() {
        assertThat(
            resolveSnapshotRevealImageRenderMode(
                shouldShowHiddenTreatment = true,
                sdkInt = Build.VERSION_CODES.R,
            ),
        ).isEqualTo(SnapshotRevealImageRenderMode.LegacyTransformation)
    }

    @Test
    fun `hidden snapshot uses compose blur on android 12 and higher`() {
        assertThat(
            resolveSnapshotRevealImageRenderMode(
                shouldShowHiddenTreatment = true,
                sdkInt = Build.VERSION_CODES.S,
            ),
        ).isEqualTo(SnapshotRevealImageRenderMode.ComposeBlur)
    }
}
