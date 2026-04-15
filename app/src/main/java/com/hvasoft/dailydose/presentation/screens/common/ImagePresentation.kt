package com.hvasoft.dailydose.presentation.screens.common

const val DefaultImageAspectRatio = 0.8f
private const val MinImageAspectRatio = 0.8f
private const val MaxImageAspectRatio = 1.78f

fun calculateClampedAspectRatio(width: Int, height: Int): Float {
    if (width <= 0 || height <= 0) return DefaultImageAspectRatio

    val rawAspectRatio = width.toFloat() / height.toFloat()
    return rawAspectRatio.coerceIn(MinImageAspectRatio, MaxImageAspectRatio)
}
