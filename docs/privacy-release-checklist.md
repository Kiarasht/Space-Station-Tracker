# Privacy and release checklist

Code can provide safe defaults, but store-console declarations and consent messages must be reviewed for every release.

## Both platforms

- Confirm `version.properties` contains a monotonically increasing build number.
- Revoke the old N2YO credential that remains in repository history (the apps no longer use N2YO) and configure platform release secrets.
- Test UMP in EEA and non-EEA debug geographies.
- Confirm banners request immediately after consent and app-open ads do not show before foreground open five.
- Verify the in-app privacy policy matches enabled SDKs and data providers.
- Run `python3 tools/sync_legal_content.py` to check that the Android legal resources match the shared/iOS text. After changing the shared text, run it with `--write`, and with `--website ../restartapps.com` to regenerate the website's ISS Tracker pages.
- Keep the public policy at `https://restartapps.com/apps/iss-tracker/privacy` (the restartapps.com repository) in step with the in-app text, and use that URL in both store listings. The old `https://kiarasht.github.io/iss_tracker_privacy.html` page is dated July 16, 2022 and should no longer be linked.
- Verify and record the actual Google Analytics retention setting and support-email retention practice. The app repository cannot determine console settings; do not claim that uninstalling the app deletes historical provider records.
- Decide how to obtain, store, withdraw, and apply Analytics/Crashlytics consent where required. Both currently initialize independently of UMP/ATT and there is no in-app telemetry switch; updating policy wording does not change collection behavior.
- Use only Google test ad units in debug and automated tests.

## Google Play

- Keep Data safety answers aligned with AdMob, location, notifications, and third-party APIs.
- Configure UMP consent messages and privacy options in AdMob.
- Declare Google Analytics for Firebase usage in Google Play Data safety and Apple App Privacy. Review Firebase's current disclosure guidance for identifiers, product interaction, advertising data, approximate location, and diagnostics before every release.
- Include Firebase Crashlytics crash data and installation/device identifiers. Reconcile AdMob's documented collection and sharing with the public Data safety answers; do not assume SDK data is excluded because the app has no account.
- Keep custom analytics parameters limited to the fixed, non-sensitive vocabulary in `AppAnalytics.kt` and `AppAnalyticsService.swift`; never add coordinates, location names, URLs, people names, transaction identifiers, user-entered text, or raw errors.
- Confirm Analytics ad-personalization signals remain disabled by default on both platforms unless the privacy policy, consent flow, and store disclosures are intentionally updated.
- Confirm the app is not declared as child-directed unless the product and ad configuration are changed accordingly.
- Use Play In-App Review only after meaningful use; do not ask users for a positive rating first.

## App Store

- Supply a final bundle identifier, Apple development team, App Store record, and iOS-specific production AdMob app/banner/app-open/Passes-native/Crew-native IDs.
- Configure `remove_ads_lifetime` as a non-consumable/one-time product in App Store Connect and Google Play Console.
- Confirm the production values in `Release.xcconfig` still match the iOS AdMob app before each release.
- Configure applicable regional AdMob messages and test the actual flow. The current app requests ATT itself before preparing UMP consent; an optional AdMob IDFA explainer does not own the system prompt.
- Review App Privacy answers for location, identifiers, usage data, diagnostics, and third-party advertising.
- Generate and inspect Xcode's privacy report. Keep `PrivacyInfo.xcprivacy` and third-party SDK privacy manifests current.
- Inspect the aggregate privacy report rather than treating the app's empty collected-data array as a claim that embedded SDKs collect nothing. Check the shipped SDK manifests, App Store Connect answers, ATT behavior, and written policy together.
- Verify the configured App Store URL (`https://apps.apple.com/app/id6803000019`) opens the production listing before release. Before the listing is published, the URL may not resolve publicly.
