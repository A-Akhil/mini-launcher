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

package com.minifocus.launcher.model

/**
 * Defines what happens when the user's intended app usage time expires.
 *
 * NOTIFICATION - A notification is shown reminding the user their time is up.
 * PROMPT       - The user is brought back to the launcher and a dialog is shown
 *                with a cooldown timer, asking them to choose more time or leave.
 */
enum class ExpiryAction {
    NOTIFICATION,
    PROMPT,
    RETURN_HOME
}
