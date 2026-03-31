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

import androidx.compose.ui.graphics.Color

// Core colors (used only in theme definitions)
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)

// AMOLED Theme Colors
val AmoledBackground = Color(0xFF000000)          // Pure black
val AmoledSurface = Color(0xFF0D0D0D)             // Slightly lighter for cards
val AmoledPrimaryText = Color(0xFFFFFFFF)         // White
val AmoledSecondaryText = Color(0xFFAAAAAA)       // Gray
val AmoledTertiaryText = Color(0xFF777777)        // Dimmed gray
val AmoledDisabledText = Color(0xFF555555)        // Very dim gray
val AmoledAccent = Color(0xFF4CAF50)              // Green accent
val AmoledError = Color(0xFFCF6679)               // Red for errors
val AmoledSuccess = Color(0xFF66BB6A)             // Green for success
val AmoledWarning = Color(0xFFFFB74D)             // Orange for warnings
val AmoledInfo = Color(0xFF64B5F6)                // Blue for info

// Light Theme Colors
val LightBackground = Color(0xFFFAFAFA)           // Light gray
val LightSurface = Color(0xFFEEEEEE)              // Slightly darker for cards
val LightPrimaryText = Color(0xFF000000)          // Black
val LightSecondaryText = Color(0xFF555555)        // Dark gray
val LightTertiaryText = Color(0xFF777777)         // Medium gray
val LightDisabledText = Color(0xFFAAAAAA)         // Light gray
val LightAccent = Color(0xFF388E3C)               // Darker green for light background
val LightError = Color(0xFFB00020)                // Red for errors
val LightSuccess = Color(0xFF388E3C)              // Green for success
val LightWarning = Color(0xFFE65100)              // Orange for warnings
val LightInfo = Color(0xFF1976D2)                 // Blue for info
