package com.minifocus.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalTextMultiplier = compositionLocalOf { 1.0f }

@Composable
fun TextSizeProvider(
    multiplier: Float,
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    val newDensity = Density(
        density = currentDensity.density,
        fontScale = multiplier
    )

    CompositionLocalProvider(
        LocalTextMultiplier provides multiplier,
        LocalDensity provides newDensity
    ) {
        content()
    }
}
