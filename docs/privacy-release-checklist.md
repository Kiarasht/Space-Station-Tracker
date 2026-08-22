# Privacy and release checklist

Code can provide safe defaults, but store-console declarations and consent messages must be reviewed for every release.

## Both platforms

- Confirm `version.properties` contains a monotonically increasing build number.
- Rotate the N2YO credential previously present in repository history and configure platform release secrets.
- Test UMP in EEA and non-EEA debug geographies.
- Confirm banners request immediately after consent and app-open ads do not show before foreground open five.
- Verify the in-app privacy policy matches enabled SDKs and data providers.
- Use only Google test ad units in debug and automated tests.

## Google Play

- Keep Data safety answers aligned with AdMob, location, notifications, and third-party APIs.
- Configure UMP consent messages and privacy options in AdMob.
- Declare Google Analytics for Firebase usage in Google Play Data safety and Apple App Privacy. Review Firebase's current disclosure guidance for identifiers, product interaction, advertising data, approximate location, and diagnostics before every release.
- Keep custom analytics parameters limited to the fixed, non-sensitive vocabulary in `AppAnalytics.kt` and `AppAnalyticsService.swift`; never add coordinates, location names, URLs, people names, transaction identifiers, user-entered text, or raw errors.
- Confirm Analytics ad-personalization signals remain disabled by default on both platforms unless the privacy policy, consent flow, and store disclosures are intentionally updated.
- Confirm the app is not declared as child-directed unless the product and ad configuration are changed accordingly.
- Use Play In-App Review only after meaningful use; do not ask users for a positive rating first.

## App Store

- Supply a final bundle identifier, Apple development team, App Store record, and iOS-specific production AdMob app/banner/app-open/Passes-native/Crew-native IDs.
- Configure `remove_ads_lifetime` as a non-consumable/one-time product in App Store Connect and Google Play Console.
- Confirm the production values in `Release.xcconfig` still match the iOS AdMob app before each release.
- Configure GDPR and IDFA messages in AdMob. UMP should control when the App Tracking Transparency prompt is eligible to appear.
- Review App Privacy answers for location, identifiers, usage data, diagnostics, and third-party advertising.
- Generate and inspect Xcode's privacy report. Keep `PrivacyInfo.xcprivacy` and third-party SDK privacy manifests current.
- Verify the configured App Store URL (`https://apps.apple.com/app/id6803000019`) opens the production listing before release. Before the listing is published, the URL may not resolve publicly.
