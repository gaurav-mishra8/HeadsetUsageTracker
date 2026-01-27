# Headphone Tracker Android App

An Android application that tracks the amount of time spent listening to headphones across all apps in a day and presents the data in graphical and numerical formats.

## Features

- **Real-time Tracking**: Monitors headphone usage across all apps in real-time
- **Daily Statistics**: View today's total headphone usage time
- **App-wise Breakdown**: See which apps you use most with headphones
- **Visual Charts**: 
  - Pie chart showing usage distribution by app
  - Bar chart showing daily usage over the last 7 days
- **Detailed App List**: View individual app usage with icons and percentages
- **Background Service**: Continues tracking even when the app is in the background

## Requirements

- Android 8.0 (API level 26) or higher
- Usage Access permission (required for tracking app usage)
- Notification permission (required for foreground service)

## Setup Instructions

1. **Clone or open the project** in Android Studio

2. **Sync Gradle** dependencies

3. **Build the project**:
   ```bash
   ./gradlew build
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

## Permissions

The app requires the following permissions:

1. **Usage Access Permission**: 
   - Required to track which apps are being used
   - The app will prompt you to grant this permission on first launch
   - Go to Settings > Apps > Special app access > Usage access

2. **Notification Permission** (Android 13+):
   - Required for the foreground service that tracks usage
   - The app will prompt you when you start tracking

## How to Use

1. **Launch the app** - You'll be prompted to grant Usage Access permission
2. **Grant permissions** - Follow the on-screen instructions
3. **Start Tracking** - Tap the "Start Tracking" button
4. **Use your headphones** - The app will automatically track usage across all apps
5. **View Statistics** - See real-time updates in the main screen:
   - Today's total usage time
   - Pie chart showing app distribution
   - Bar chart showing last 7 days
   - Detailed app list with individual usage times

## Technical Details

### Architecture
- **Language**: Kotlin
- **Database**: Room Database for local storage
- **UI**: Material Design Components with View Binding
- **Charts**: MPAndroidChart library
- **Background Service**: LifecycleService for tracking

### Key Components
- `MainActivity`: Main UI with charts and statistics
- `HeadphoneTrackingService`: Background service that tracks usage
- `AppDatabase`: Room database for storing usage data
- `HeadphoneUsageDao`: Data access object for queries

### Data Storage
- Usage data is stored locally in Room database
- Data is organized by date (YYYY-MM-DD format)
- Each usage session records:
  - App package name and name
  - Start and end time
  - Duration
  - Date

## Troubleshooting

### App not tracking usage
- Ensure Usage Access permission is granted
- Check if the tracking service is running (notification should be visible)
- Verify headphones are connected

### Charts not showing data
- Make sure you've used headphones with apps
- Wait a few seconds for data to be collected
- Check if tracking is enabled

### Permission issues
- Go to Settings > Apps > Headphone Tracker > Permissions
- Enable Usage Access permission
- On Android 13+, enable Notification permission

## Building from Source

1. Ensure you have Android Studio installed
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the project (Build > Make Project)
5. Run on device or emulator

## Dependencies

- AndroidX Core KTX
- Material Design Components
- Room Database
- MPAndroidChart
- Kotlin Coroutines
- Lifecycle Services

## License

This project is open source and available for educational purposes.

## Notes

- The app tracks usage only when headphones are connected
- Tracking works across all apps that play audio
- Data is stored locally on your device
- The service runs in the foreground with a notification

## Release & CI: signing and publishing

This repo includes workflows to build signed releases and publish to Google Play (internal track).

Required GitHub Secrets (Repository -> Settings -> Secrets & variables -> Actions):

- `KEYSTORE_BASE64` — Base64-encoded bytes of your keystore (e.g. `cat release-keystore.jks | base64`).
- `KEYSTORE_PASSWORD` — Keystore password.
- `KEY_ALIAS` — Key alias inside the keystore.
- `KEY_PASSWORD` — Key password for the alias.
- `PLAY_STORE_SERVICE_ACCOUNT_JSON` — The JSON content of a Google Play service account with the `Service Account User` and `Release Manager` roles.

How to trigger:
- Push a tag matching `v*` (for example `git tag v1.0.0 && git push origin v1.0.0`) to run the publish workflow automatically.
- Or trigger the workflow manually in the Actions tab using `workflow_dispatch`.

Notes and alternatives:
- You can also run signed builds locally by passing Gradle properties:
```bash
./gradlew :app:assembleRelease \
   -PkeystorePath=/path/to/keystore.jks \
   -PkeystorePassword=... \
   -PkeyAlias=... \
   -PkeyPassword=...
```
- If you prefer not to store a keystore in Actions, consider using Google Play App Signing and upload an unsigned AAB instead.


