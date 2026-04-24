package com.hvasoft.dailydose.presentation.screens.home.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.max

private const val LegacyRevealSampling = 80

internal enum class SnapshotRevealImageRenderMode {
    Default,
    ComposeBlur,
    LegacyTransformation,
}

internal fun resolveSnapshotRevealImageRenderMode(
    shouldShowHiddenTreatment: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): SnapshotRevealImageRenderMode = when {
    !shouldShowHiddenTreatment -> SnapshotRevealImageRenderMode.Default
    sdkInt >= Build.VERSION_CODES.S -> SnapshotRevealImageRenderMode.ComposeBlur
    else -> SnapshotRevealImageRenderMode.LegacyTransformation
}

internal fun buildSnapshotRevealImageModel(
    context: Context,
    originalModel: Any?,
    renderMode: SnapshotRevealImageRenderMode,
): Any? {
    if (originalModel == null) return null
    if (renderMode != SnapshotRevealImageRenderMode.LegacyTransformation) {
        return originalModel
    }

    return ImageRequest.Builder(context)
        .data(originalModel)
        .allowHardware(false)
        .transformations(LegacyRevealObscureTransformation())
        .build()
}

internal class LegacyRevealObscureTransformation(
    private val sampling: Int = LegacyRevealSampling,
) : Transformation {

    init {
        require(sampling > 1) { "sampling must be greater than 1" }
    }

    override val cacheKey: String = "snapshot_hidden_legacy_v1_sampling=$sampling"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val reducedWidth = max(1, input.width / sampling)
        val reducedHeight = max(1, input.height / sampling)
        val downscaled = Bitmap.createScaledBitmap(input, reducedWidth, reducedHeight, true)
        val output = Bitmap.createBitmap(
            input.width,
            input.height,
            input.config?.takeUnless { it == Bitmap.Config.HARDWARE } ?: Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(
            downscaled,
            null,
            android.graphics.Rect(0, 0, input.width, input.height),
            paint,
        )

        if (downscaled !== input && !downscaled.isRecycled) {
            downscaled.recycle()
        }
        return output
    }

    override fun equals(other: Any?): Boolean =
        other is LegacyRevealObscureTransformation && other.sampling == sampling

    override fun hashCode(): Int = cacheKey.hashCode()
}
