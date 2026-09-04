# iOS release setup

The generated Xcode project is configured for ISS Tracker 7.10 (54), the `com.restart.spacestationtracker.ios` release bundle identifier, and Apple team `UT45N57AJ6`, matching the USAJobs signing baseline.

## Before the first App Store archive

1. Create or confirm the App Store Connect app record and bundle identifier.
2. Create a separate iOS app in AdMob. Do not reuse the Android app or Android ad-unit IDs.
3. Copy `iosApp/Configuration/Local.xcconfig.example` to `Local.xcconfig` for local signing or overrides. Release AdMob app, banner, app-open, Passes-native, and Crew-native identifiers are declared in `Release.xcconfig`.
4. Configure `remove_ads_lifetime` as a non-consumable in-app purchase in App Store Connect and set its storefront price to $9.99.
5. The App Store listing URL is configured from Apple ID `6803000019` as `https://apps.apple.com/app/id6803000019`.
6. Configure applicable regional consent messages in AdMob Privacy & messaging, then verify the Privacy Choices row and ATT flow on a physical device. The app explicitly requests ATT before gathering UMP consent; an AdMob IDFA explainer is not required to trigger Apple's prompt.
7. Review App Store Privacy answers against AdMob, UMP, approximate location sent to the pass provider, local settings, notifications, and calendar actions.

Release ads are enabled with the iOS-specific production AdMob application and ad-unit identifiers in `Release.xcconfig`. Debug builds continue to use Google's official test identifiers.

## ATT review verification

The iOS app waits until active, requests Apple's tracking authorization if undecided, then gathers UMP consent before initializing ads. An existing allowed, denied, or restricted decision is not prompted again. Denial does not block app functionality or ads permitted by UMP without IDFA. Restored ad-free purchases skip ad-related permission requests. Firebase usage analytics remains unchanged by this flow.

Before resubmitting, verify on a physical iPhone and iPad:

1. Use a fresh install/reset tracking-permission state with **Settings > Privacy & Security > Tracking > Allow Apps to Request to Track** enabled and an eligible account. Use an account without an existing ad-free purchase for this flow.
2. Record launch, the system ATT prompt, a selection, any applicable regional consent form, and the app continuing normally. Ads must not load while ATT is undecided.
3. Verify both Allow and Ask App Not to Track, relaunch without a repeated prompt, and verify returning from background or another permission dialog does not cause duplicate prompts.
4. With tracking requests disabled/restricted, verify the app remains usable without attempting to force a prompt.
5. Attach the physical-device recording to the App Review response and reference it in App Review Information Notes. Reconcile App Store privacy answers with the actual SDK and advertising configuration; the prompt alone is not a complete privacy audit.

The N2YO credential is intentionally excluded from source control. Local builds read it from ignored `iosApp/Configuration/Local.xcconfig`. GitHub Actions reads the same value from the repository-level `N2YO_API_KEY` secret and generates that local config during CI. Rotate the existing N2YO key before production because removing it from the current tree does not remove it from Git history, then update the repository secret and each developer's local config with the replacement.

## GitHub release delivery

The `Mobile Production Release` workflow explicitly selects Xcode 26 and the iOS 26 SDK, archives and signs the iOS Release app, exports an IPA, retains the IPA and dSYMs as workflow artifacts, and uploads the build to App Store Connect for TestFlight processing. It reads version `7.10 (54)` from the same root `version.properties` file used by Android.

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
