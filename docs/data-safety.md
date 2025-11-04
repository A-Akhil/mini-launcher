# Data Safety Summary

**Last updated:** 4 November 2025

This document summarizes how Minimalist Focus handles user data to assist with Google Play’s Data Safety questionnaire.

## Overview

- **No data collection:** The App does not send any personal or device data to external servers.
- **No sharing:** We do not share data with third parties.
- **On-device processing:** All features (tasks, hidden apps, notification inbox, app locks) operate entirely on-device.
- **No mandatory sign-in:** Users are not required to create an account.

## Data Collection Table

| Data Type | Collected | Shared | Purpose | Handling |
|-----------|-----------|--------|---------|----------|
| Personal info (name, email, etc.) | No | No | Not applicable | Not collected |
| Location | No | No | Not applicable | Not collected |
| Contacts | No | No | Not applicable | Not collected |
| Photos or media | No | No | Not applicable | Not collected |
| Files or docs | No | No | Not applicable | Not collected |
| Calendar | No | No | Not applicable | Not collected |
| App activity | No (except local lock state) | No | Not applicable | Lock state stored locally only |
| Web browsing | No | No | Not applicable | Not collected |
| App info and performance | No remote analytics | No | Not applicable | Not collected |
| Device or other IDs | No | No | Not applicable | Not collected |

## Optional Features and Local Storage

| Feature | Data Involved | Storage | Shared? |
|---------|---------------|---------|---------|
| Tasks list | Task titles entered by the user | Stored locally via Room database | No |
| Hidden apps | Package names of hidden apps | Stored locally via Room database | No |
| App locks | Package names and lock timers | Stored locally via Room database | No |
| Notification inbox (optional) | Notification content received on device | Stored locally via Room database | No |
| Settings | User preferences (booleans, numbers) | Stored locally via DataStore | No |

All local databases are stored in the App’s private sandboxed storage. Removing the App deletes this data.

## Data Encryption and Retention

- **Encryption:** Android automatically encrypts app sandbox storage on devices with file-based encryption. The App does not implement additional encryption.
- **Retention:** Data is retained only while the App is installed. Users can clear data via Android settings or uninstall the App to delete everything.

## Access Permissions

| Permission | Purpose | Data Flow |
|------------|---------|-----------|
| `QUERY_ALL_PACKAGES` | Display installed apps in launcher | Local only |
| `PACKAGE_USAGE_STATS` | Enforce app locks by monitoring launches | Local only |
| `SYSTEM_ALERT_WINDOW` | Show lock overlay on top of other apps | Local only |
| `POST_NOTIFICATIONS` | Deliver reminders/alerts | Local only |
| Notification Listener | Optional inbox feature to organize notifications | Local only |

## Policy Compliance Statements

- No data is collected for advertising or analytics.
- No data is shared with third parties.
- The App does not facilitate external data transfers.
- Users can disable optional permissions at any time; related features will stop functioning but no data will be transmitted elsewhere.
- The App is not directed to children under 13.
