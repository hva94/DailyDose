package com.hvasoft.dailydose.presentation.screens.home.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.size.Size
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyRevealObscureTransformationTest {

    @Test
    fun transform_preserves_dimensions_and_obscures_non_uniform_bitmaps() = runBlocking {
        val input = createQuadrantBitmap(size = 96)

        val transformed = LegacyRevealObscureTransformation().transform(
            input = input,
            size = Size(input.width, input.height),
        )

        assertEquals(input.width, transformed.width)
        assertEquals(input.height, transformed.height)
        assertFalse(transformed.sameAs(input))
    }

    private fun createQuadrantBitmap(size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            val half = size / 2
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val color = when {
                        x < half && y < half -> Color.RED
                        x >= half && y < half -> Color.GREEN
                        x < half && y >= half -> Color.BLUE
                        else -> Color.YELLOW
                    }
                    setPixel(x, y, color)
                }
            }
        }
}
