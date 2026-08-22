# ISS Tracker

ISS Tracker follows the International Space Station in real time, finds visible passes, schedules pass alerts, shows the current crew, and links to NASA live streams.

## Project status

Version `7.08` completes the main Kotlin Multiplatform product migration.

- `app`: Android navigation and platform integrations around shared Compose screens
- `shared`: KMP networking, models, policies, presentation state, resources, theme, and Compose UI
- `iosApp`: SwiftUI platform host backed by the same `ISSTrackerShared` product UI
- `version.properties`: single version name and build number source for Gradle and Xcode

Android retains its existing DataStore and SharedPreferences file/key contracts so upgrades preserve settings, alerts, review state, and app-open counts. Legacy rewarded ad-free timers are removed during upgrade. The lifetime ad-removal purchase is restored from Google Play or the App Store. iOS uses `NSUserDefaults` with matching shared settings models.

## Build

Compile/package checks used by CI:

```sh
./gradlew :app:assembleDebug :shared:compileKotlinIosSimulatorArm64
```

Generate the iOS project after editing `iosApp/project.yml`:

```sh
cd iosApp
xcodegen generate --spec project.yml
```

Build the iOS simulator app:

```sh
xcodebuild \
  -project iosApp/ISSTrackerIOS.xcodeproj \
  -scheme ISSTrackerIOS \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Versioning

Update only `version.properties`:

```properties
APP_VERSION_NAME=7.08
APP_VERSION_CODE=52
```

Android reads it during Gradle configuration. Both Xcode configurations include the same file directly.

See [docs/kmp-migration.md](docs/kmp-migration.md) for the phased migration plan and [docs/privacy-release-checklist.md](docs/privacy-release-checklist.md) for required store and consent checks.

Android closed-testing publishing is documented in [docs/android-play-publishing.md](docs/android-play-publishing.md).

iOS signing, production configuration, and archiving are documented in [docs/ios-release-setup.md](docs/ios-release-setup.md).
