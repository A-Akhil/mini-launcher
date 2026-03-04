/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
