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
