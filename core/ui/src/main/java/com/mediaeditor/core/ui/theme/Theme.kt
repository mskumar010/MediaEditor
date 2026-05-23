package com.mediaeditor.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun HybridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HybridDarkColorScheme else HybridLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = HybridShapes,
        typography = HybridTypography,
        content = content
    )
}
