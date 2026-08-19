# KMP migration plan

## Compatibility rules

The Android application ID remains `com.restart.spacestationtracker`. Moving a class to `shared` keeps its existing Kotlin package, preserving its JVM fully qualified name.

The following Android storage contracts are frozen until a tested migration replaces them:

| Data | Storage | Compatibility requirement |
|---|---|---|
| App settings and alert configuration | DataStore `settings` | Keep file name and preference keys |
| App-open ad count | SharedPreferences `app_open_ads`, key `foreground_open_count` | Used by the shared policy repository |
| Review eligibility | SharedPreferences `app_rating`, existing review keys | Used by the shared review repository |
| Scheduled automatic alerts | Existing settings keys and Android alarm IDs | Do not rename before migration/reconciliation |

Model migrations must either retain the same serialized shape or provide tolerant decoding with defaults and tests. Transient API models can move first; persisted models move only with fixture-based compatibility tests.

## Completed in 7.07

- Added Android/iOS KMP targets and the SwiftUI host.
- Aligned Gradle to `9.3.1`, matching the working USAJobs KMP baseline and Kotlin/Hilt metadata compatibility.
- Added a root version file consumed by both platforms.
- Moved safe ISS location, crew, expedition, and NASA stream domain code to `commonMain`.
- Shared app-open and review policies while retaining Android storage names and keys.
- Added Google Play in-app review for Play installs and the existing Galaxy Store path.
- Added iOS StoreKit review eligibility.
- Added UMP consent, adaptive banner, and fifth-open app-open behavior to the iOS host.
- Forced Android debug builds to use Google test ad units.
- Added an Apple privacy manifest and restored Firebase Analytics on Android and iOS with a shared, privacy-safe event vocabulary. Release diagnostic logging remains removed while structured usage analytics stays enabled.
- Replaced the Android Retrofit/Gson data layer with shared Ktor and Kotlin Serialization.
- Moved ISS position, visible-pass, crew, Wikipedia biography, and NASA stream repositories into `commonMain`.
- Replaced `java.util.Date` in the pass domain model with a multiplatform epoch-millisecond contract.
- Added shared Compose Multiplatform navigation and working Map, Sky Path, On Duty, Settings, and About screens.
- Connected the iOS host to shared Compose UI instead of maintaining a placeholder SwiftUI product.
- Added thin iOS adapters for MapKit, Core Location, StoreKit review, UMP, and AdMob.
- Added a shared settings model with the frozen Android DataStore key contract; iOS persists the same fields in `NSUserDefaults`.
- Added iOS manual pass notifications, calendar export, and system sharing from shared pass cards.
- Added automatic iOS pass-alert reconciliation with stored coordinates and `BGAppRefreshTask`.
- Added Compose Multiplatform resources and moved navigation/pass strings plus all existing Android pass translations into `commonMain`.
- Completed shared localization coverage for Crew, Settings, and About across every supported locale.
- Ported the Android sky-path geometry to a shared Compose canvas used by iOS, including start/end compass points, observer position, peak elevation, and visibility.
- Moved pass-magnitude classification into tested common Kotlin and made Android delegate to the shared thresholds.
- Switched Android pass cards to the shared Compose implementation while retaining Android native-ad insertion, permission prompts, alarm scheduling, calendar intents, and sharing.
- Switched Android Map to the shared Compose implementation while retaining Google Maps, lifetime ad removal, stream launching, map settings, and the Android navigation/banner shell.
- Switched Android Crew to shared Compose cards and feed rendering while preserving native-ad insertion through platform slots.
- Replaced the minimal shared Settings placeholder with the full KMP settings surface: automatic alerts, alert health, minimum visibility, notification times, saved location, background reliability, map type, orbit path, theme, and privacy choices.
- Kept Android permission prompts, location capture, WorkManager scheduling, alarm cleanup, battery settings, and UMP consent behind platform callbacks.
- Moved Android's navy/yellow light and dark palettes plus Orbitron/Exo typography into the shared KMP theme, making Android and iOS render from one design system.
- Replaced the iOS About placeholder with the shared Android-style About layout and native support, rating, sharing, privacy, and terms actions on both platforms.
- Added iOS-native AdMob placements inside the shared Passes and Crew lists, plus a permanent ad-removal purchase used by both platforms.
- Removed legacy rewarded ad-free timers during upgrade and store the permanent entitlement as `ad_removal_lifetime_enabled`.
- Kept Android's native navigation and platform adapters while sharing its product screens and Ktor repositories, preserving Android-specific ads, alarms, notifications, calendar actions, and existing storage.
- Moved the N2YO credential out of tracked Kotlin and into platform-local/release configuration, with release validation and CI secret wiring.
- Removed superseded Android Crew rendering and its obsolete Coil 2, Glide, and app-level OkHttp stack.
- Migrated the Android app module to AGP built-in Kotlin, matching the current USAJobs baseline.

## Platform boundaries retained intentionally

- Android keeps WorkManager, AlarmManager, notification permission, Google Maps, calendar intents, and native AdMob views as platform adapters.
- iOS keeps BGTaskScheduler, UserNotifications, MapKit, EventKit, StoreKit, UMP, and native AdMob views as platform adapters.
- The platform adapters consume shared models, visibility/lead-time policy, settings contracts, repositories, theme, resources, and Compose screens.

## Remaining release gates

1. Rotate the N2YO key because its previous value remains in Git history, then configure `N2YO_API_KEY` in GitHub Actions and `ISS_TRACKER_N2YO_API_KEY` for iOS archives.
2. Create/confirm the App Store record and iOS-specific AdMob app plus ad units, then enable release ads.
3. Exercise notification, calendar, location, consent, ad, review, and upgrade flows on physical Android and iOS devices before store submission.
4. Complete the Play Console and App Store privacy declarations from the release checklist.

Automated unit/UI tests are retained in source but are not part of the current requested verification workflow.
