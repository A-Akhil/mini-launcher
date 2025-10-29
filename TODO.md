# TODO - Feature Backlog

## 1. Double Tap to Turn Off Screen
**Status:** Not Started

### Description
Implement double-tap gesture on home screen to turn off the device screen.

### Known Challenges
- **Permission Issues:** Turning off the screen requires special permissions
  - `DEVICE_ADMIN` permission or Device Administrator policy
  - May require user to grant Device Admin privileges in Settings
  - Global permission onboarding flow (2025-10-27) now surfaces the device admin request at app launch; gesture logic still pending.
  - Exact alarm capability has been added to onboarding to support timely reminders once gesture-triggered locks interact with scheduling.

### Technical Approach
1. Implement device admin receiver for screen-off capability
2. Add double-tap gesture detector on home screen
3. Request device admin permission on first use
4. Handle permission denial gracefully with user guidance

### Dependencies
- Android DevicePolicyManager
- Device Admin manifest declarations

---

## 2. Configurable Text Size
**Status:** Not Started

### Description
Allow users to customize font sizes across the launcher interface.

### Scope
- Task list text
- App drawer labels
- Home screen clock
- Settings page text
- Dialog text

### Technical Approach
1. Add font size preference to SettingsManager (DataStore)
2. Create font size selector in settings page (e.g., Small, Medium, Large, Extra Large)
3. Define scaling factors in theme or composable constants
4. Apply dynamic text size modifiers throughout UI
5. Persist user preference

### UI Location
Settings page → "Text Size" option with preview

---

## 3. Customizable Home Screen Bottom Icons
**Status:** Completed

### Description
Allow users to customize both left and right bottom icons on home screen (currently Phone and Camera).

### Requirements
- Let user pick any installed app for left icon
- Let user pick any installed app for right icon
- Provide clear UI to change assignments
- Persist selections in database
---

## 4. Overview/Recents Black Background
**Status:** Not Started

### Description
Ensure the system Overview (Recents) screen shows the launcher's intended pure black background instead of the device's default wallpaper.

### Considerations
- Current behavior: overview preview displays system wallpaper when multitasking.
- Desired behavior: maintain minimalist solid black aesthetic across launcher and system overview.
- May require adjustments to window/task snapshot configuration in `MainActivity` or theme.


---

Here is the new feature added to your backlog as item #5:

---

## 5. Notification Filter & Inbox
**Status:** Not Started

### Description
Implement a notification filtering system that blocks all incoming system notifications. All intercepted notifications will be stored in a notification history/inbox within the launcher, which the user must manually open to view.

### Known Challenges
- **Permission Issues:** This feature requires "Notification Access" (`NotificationListenerService`), which is a sensitive permission. The user must grant this manually through a specific system settings page.
- **Service Reliability:** The `NotificationListenerService` can be stopped by the system and needs to be robust.
- **Data Storage:** Storing a history of all notifications can lead to a large database. A data retention and purging strategy will be needed.
  - Permission onboarding (2025-10-27) already directs users to enable notification access; service implementation must leverage the granted access when delivered.
  - Permission screen informs sideload users how to enable "Allow restricted settings" and links to App Info for convenience.
  - Exact alarm privilege is now requested alongside other permissions so scheduled reminders can alert at precise times.

### Technical Approach
1. Create a class that extends `NotificationListenerService`.
2. In the `onNotificationPosted(StatusBarNotification sbn)` method:
   - Extract relevant data (app icon, app name, title, text, time) from the `sbn` object.
   - Store this data in a local database (e.g., Room).
   - Call `cancelNotification(sbn.getKey())` to immediately dismiss the notification so the user doesn't see it.
3. Create a new UI ("Notification Inbox") in the launcher to display the stored notification history from the database.
4. On first use, check for permission. If not granted, provide a clear explanation and a button that sends an `Intent` for `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` to guide the user to the correct settings page.

### Dependencies
- `NotificationListenerService` API
- `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` manifest declaration
- Room Database for storing notification history

---

## 6. Fix Notification Badge Clipping in All Apps Header
**Status:** Not Started  
**Priority:** High

### Description
The red notification badge (unread count indicator) on the notification bell icon in the All Apps header appears clipped or invisible. Multiple layout approaches have been attempted (BadgedBox, Box with offset, Box with padding), but the badge consistently renders with its boundary cut off.

### Known Issues
- Badge rendering issue persists across different Compose layout strategies
- Problem may be device/density-specific or related to parent Box constraints
- Current implementation uses inner Box with padding (3.dp top/end) but user reports badge still not fully visible
---

## 7. Long Click to Uninstall in App Options
**Status:** Not Started

### Description
Add "Uninstall" as a long-click option alongside existing actions (e.g., lock, hide, pin) in the app options menu for each app in the launcher.

### Technical Approach
1. Add "Uninstall" to the long-press menu for app entries.
2. Use the standard uninstall intent for the selected package.
3. Handle uninstall result to refresh app list and update UI.

---

## 8. Enhanced App Lock (Persistent Overlay)
**Status:** Completed

### Description
App lock should prevent launching a locked app until the unlock time, even if the user switches to another launcher and returns. When a locked app is attempted to be opened, show a persistent overlay with a motivational message and unlock time.

### Requirements
- Overlay must appear when launching a locked app from our launcher.
- Overlay must also appear if the user tries to open the locked app from another launcher (as long as our launcher is still installed).
- Overlay screen should display:
  - App name (e.g., "App X is locked for 30 min")
  - Motivational message (static, e.g., "You chose to focus / Respect your decision")
  - Countdown timer showing remaining time
  - Unlock time
- Overlay must block interaction with the app until the lock expires.
- No unlock button - enforces digital detox.
- Emergency unlock only accessible through hidden 20-click sequence in About screen.

### Implementation Status
Created `AppLockMonitorService` - Foreground service using `UsageStatsManager` to detect app launches
Created `AppLockOverlayActivity` - Black screen with motivational message, countdown timer, and unlock time
Added `PACKAGE_USAGE_STATS` permission to manifest
Added `SYSTEM_ALERT_WINDOW` (overlay) permission to permission flow
Added usage stats permission to `PermissionScreen` and permission flow
Added overlay permission to `PermissionScreen` and permission flow
Service automatically starts/stops based on active lock state
Monitoring uses Handler.postDelayed (no infinite loop, checks every 1 second)
Works across launchers - intercepts app launches system-wide when our launcher is installed
**Removed "Go to Home" button - overlay auto-dismisses when timer expires**
**Back button completely blocked - user cannot escape the lock screen**
**Removed unlock button from lock screen overlay**
**Removed unlock option from long-press context menus**
**Emergency unlock system implemented:**
---

## 9. Consistent Back Button & Header Behavior
**Status:** Completed (2025-10-29)

### Description
Unify the back button and header UI across all screens. The back button should always return to the previous screen (not always home), and header styling should be consistent (icon, placement, text size, etc.).

---

## Future Enhancements
- [ ] Widget support on home screen (But the thing is we need in black and white theme)
- [ ] Gesture customization (swipe actions)
- [ ] Theme color customization
- [ ] Backup/restore settings and data
- [ ] App usage statistics integration
