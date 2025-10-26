# TODO - Feature Backlog

## 1. Double Tap to Turn Off Screen
**Status:** Not Started

### Description
Implement double-tap gesture on home screen to turn off the device screen.

### Known Challenges
- **Permission Issues:** Turning off the screen requires special permissions
  - `DEVICE_ADMIN` permission or Device Administrator policy
  - May require user to grant Device Admin privileges in Settings

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
**Status:** Not Started

### Description
Allow users to customize both left and right bottom icons on home screen (currently Phone and Camera).

### Requirements
- Let user pick any installed app for left icon
- Let user pick any installed app for right icon
- Provide clear UI to change assignments
- Persist selections in database

### Technical Approach
1. Extend Room schema with `bottom_left_app` and `bottom_right_app` fields (or new table)
2. Add two new options in Settings page: "Left Bottom Icon" and "Right Bottom Icon"
3. Tapping each option opens app picker dialog showing all installed apps
4. Update ViewModel to expose bottom icon state
5. Render selected apps' icons and labels dynamically on home screen

### UI Flow
- Settings page → "Left Bottom Icon" option → Shows current app (default: Phone)
- Tap option → Opens app picker dialog (similar to All Apps list)
- User selects new app → Dialog closes, selection saved
- Settings page → "Right Bottom Icon" option → Shows current app (default: Camera)
- Tap option → Opens app picker dialog
- User selects new app → Dialog closes, selection saved
- Changes reflected immediately on home screen and persist across restarts

---

## Future Enhancements
- [ ] Widget support on home screen (But the thing is we need in black and white theme)
- [ ] Gesture customization (swipe actions)
- [ ] Theme color customization
- [ ] Backup/restore settings and data
- [ ] App usage statistics integration
