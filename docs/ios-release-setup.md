# iOS release setup

The generated Xcode project is configured for ISS Tracker 7.08 (52), the `com.restart.spacestationtracker.ios` release bundle identifier, and Apple team `UT45N57AJ6`, matching the USAJobs signing baseline.

## Before the first App Store archive

1. Create or confirm the App Store Connect app record and bundle identifier.
2. Create a separate iOS app in AdMob. Do not reuse the Android app or Android ad-unit IDs.
3. Copy `iosApp/Configuration/Local.xcconfig.example` to `Local.xcconfig` for local signing or overrides. Release AdMob app, banner, app-open, Passes-native, and Crew-native identifiers are declared in `Release.xcconfig`.
4. Configure `remove_ads_lifetime` as a non-consumable in-app purchase in App Store Connect and set its storefront price to $9.99.
5. The App Store listing URL is configured from Apple ID `6803000019` as `https://apps.apple.com/app/id6803000019`.
6. Configure GDPR and IDFA messages in AdMob Privacy & messaging, then verify the Privacy Choices row and ATT flow on a physical device.
7. Review App Store Privacy answers against AdMob, UMP, approximate location sent to the pass provider, local settings, notifications, and calendar actions.

Release ads are enabled with the iOS-specific production AdMob application and ad-unit identifiers in `Release.xcconfig`. Debug builds continue to use Google's official test identifiers.

The N2YO credential is intentionally excluded from source control. Local builds read it from ignored `iosApp/Configuration/Local.xcconfig`. GitHub Actions reads the same value from the repository-level `N2YO_API_KEY` secret and generates that local config during CI. Rotate the existing N2YO key before production because removing it from the current tree does not remove it from Git history, then update the repository secret and each developer's local config with the replacement.

## GitHub release delivery

The `Mobile Production Release` workflow archives and signs the iOS Release app, exports an IPA, retains the IPA and dSYMs as workflow artifacts, and uploads the build to App Store Connect for TestFlight processing. It reads version `7.08 (52)` from the same root `version.properties` file used by Android.

Required repository secrets:

- `APP_STORE_CONNECT_API_KEY_BASE64`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APPLE_TEAM_ID`
- `IOS_APP_STORE_PROVISIONING_PROFILE_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`
- `IOS_SIGNING_KEYCHAIN_PASSWORD`
- `N2YO_API_KEY`

The workflow runs on pushes to `master` and can also be started manually from GitHub Actions. Uploading places the build in App Store Connect/TestFlight processing; it does not automatically submit the app for public App Review.

## Unsigned device build check

```sh
xcodebuild \
  -project iosApp/ISSTrackerIOS.xcodeproj \
  -scheme ISSTrackerIOS \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Archive

Once signing and production configuration are present:

```sh
xcodebuild \
  -project iosApp/ISSTrackerIOS.xcodeproj \
  -scheme ISSTrackerIOS \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath build/ISSTrackerIOS.xcarchive \
  archive
```
