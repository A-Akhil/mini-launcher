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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.minifocus.launcher.model.LauncherTheme

private val AmoledColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = AmoledSecondaryText,
    onSecondary = White,
    tertiary = AmoledAccent,
    onTertiary = Black,
    background = AmoledBackground,
    onBackground = AmoledPrimaryText,
    surface = AmoledSurface,
    onSurface = AmoledPrimaryText,
    surfaceVariant = AmoledSurface,
    onSurfaceVariant = AmoledSecondaryText,
    error = AmoledError,
    onError = Black
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = LightSecondaryText,
    onSecondary = White,
    tertiary = LightAccent,
    onTertiary = White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightSecondaryText,
    error = LightError,
    onError = White
)

@Composable
fun MinimalistFocusTheme(
    theme: LauncherTheme = LauncherTheme.AMOLED,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        LauncherTheme.AMOLED -> AmoledColorScheme
        LauncherTheme.LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
