# Headphone Tracker - Project Summary

## Overview
A complete Android application that tracks headphone usage time across all apps and displays the data in graphical and numerical formats.

## Project Structure

### Core Components

1. **MainActivity.kt**
   - Main UI with charts and statistics
   - Real-time data updates
   - Permission handling
   - Auto-refresh when tracking is active

2. **HeadphoneTrackingService.kt**
   - Background service for tracking usage
   - Monitors headphone connection state
   - Tracks app usage with audio playback
   - Stores data in Room database

3. **UsageStatsPermissionActivity.kt**
   - Handles Usage Access permission request
   - Guides users to grant necessary permissions

4. **AppUsageAdapter.kt**
   - RecyclerView adapter for app usage list
   - Displays app icons, names, and usage times

### Data Layer

1. **AppDatabase.kt**
   - Room database setup
   - Singleton pattern for database instance

2. **HeadphoneUsage.kt**
   - Entity for storing usage sessions
   - Fields: id, packageName, appName, startTime, endTime, duration, date

3. **HeadphoneUsageDao.kt**
   - Data Access Object for database queries
   - Methods for retrieving daily usage, app-wise usage, and statistics

4. **AppUsageSummary.kt**
   - Data class for aggregated app usage
   - Used in pie chart and app list

5. **DailyUsageSummary.kt**
   - Data class for daily usage statistics
   - Used in bar chart

### UI Components

1. **activity_main.xml**
   - Main screen layout
   - Total time display
   - Pie chart for app distribution
   - Bar chart for daily usage
   - App list with RecyclerView

2. **item_app_usage.xml**
   - List item layout for app usage
   - App icon, name, usage time, and percentage

3. **activity_usage_stats_permission.xml**
   - Permission request screen

### Resources

1. **strings.xml** - All text resources
2. **colors.xml** - Color definitions
3. **themes.xml** - App theme configuration

## Key Features

### Tracking
- Real-time headphone usage tracking
- Tracks across all apps
- Only tracks when headphones are connected
- Only tracks when audio is actively playing
- Background service continues tracking when app is closed

### Data Visualization
- **Pie Chart**: Shows usage distribution by app for today
- **Bar Chart**: Shows daily usage for last 7 days
- **App List**: Detailed list with icons, names, times, and percentages
- **Total Time**: Large display of today's total usage

### Data Storage
- Room database for local storage
- Data organized by date (YYYY-MM-DD)
- Stores individual usage sessions
- Aggregates data for display

## Permissions Required

1. **Usage Access Permission**
   - Required to track which apps are being used
   - User must grant manually in Settings

2. **Notification Permission** (Android 13+)
   - Required for foreground service
   - App requests automatically

## Technical Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Database**: Room
- **Charts**: MPAndroidChart
- **UI**: Material Design Components
- **Architecture**: Service-based tracking with Room database

## How It Works

1. User grants Usage Access permission
2. User starts tracking via button
3. Service monitors:
   - Headphone connection state
   - Active app (via UsageStatsManager)
   - Audio playback state
4. When headphones connected + audio playing:
   - Service tracks the active app
   - Records usage sessions
   - Stores in database
5. UI displays:
   - Real-time updates (every 5 seconds when tracking)
   - Aggregated data in charts
   - Detailed app list

## Building and Running

1. Open project in Android Studio
2. Sync Gradle files
3. Build project (Build > Make Project)
4. Run on device or emulator (API 26+)
5. Grant Usage Access permission when prompted
6. Start tracking and use headphones with apps

## Notes

- App icons are referenced but need to be generated (Android Studio can generate these)
- The service uses a wake lock to continue tracking in background
- Data is stored locally on device
- Tracking accuracy depends on system permissions and audio state detection

## Future Enhancements (Optional)

- Export data to CSV/JSON
- Set daily usage limits with alerts
- Weekly/Monthly statistics
- Widget for quick view
- Dark theme support
- Data backup/restore

