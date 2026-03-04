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

package com.minifocus.launcher.permissions

/**
 * DEPRECATED: Device admin integration removed.
 * 
 * Originally used DevicePolicyManager.lockNow() for double-tap lock feature,
 * but that approach disabled biometric unlock and forced PIN-only authentication.
 * 
 * Replaced with LockScreenAccessibilityService which uses GLOBAL_ACTION_LOCK_SCREEN
 * to preserve fingerprint/face unlock behavior like the hardware power button.
 */
