package com.hvasoft.dailydose.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    secondary = PrimaryVariantLight,
    tertiary = PrimaryVariantLight,
    onPrimary = OnPrimaryDark,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryLight,
    secondary = PrimaryVariantDark,
    tertiary = PrimaryVariantDark,
    onPrimary = OnPrimaryLight,
)

@Composable
fun DailyDoseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
