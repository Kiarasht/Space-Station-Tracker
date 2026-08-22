# iOS host

This is the native SwiftUI host for the KMP conversion.

- `version.properties` at the repository root is included directly by both Xcode configurations.
- Debug builds use Google's official iOS test identifiers for banner, app-open, and native ads.
- Lifetime ad removal uses the non-consumable StoreKit product `remove_ads_lifetime`.
- Release ads use the iOS-specific AdMob app ID plus banner, app-open, Passes-native, and Crew-native production ad-unit IDs declared in `Configuration/Release.xcconfig`.
- UMP consent is refreshed on every launch. Ads are requested only when `canRequestAds` is true.
- Banner ads can appear immediately after consent. App-open ads begin on the fifth foreground open.
- Review eligibility is shared with Android: at least three days of use, five launches, and fifteen meaningful interactions.

See [`../docs/ios-release-setup.md`](../docs/ios-release-setup.md) for App Store, signing, AdMob, and archive setup.

Generate the Xcode project after changing `project.yml`:

```sh
xcodegen generate --spec iosApp/project.yml
```
