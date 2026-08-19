# Android Play publishing

The production release workflow builds a signed Android App Bundle and uploads it to the Google Play closed-testing track `alpha`.

## Required GitHub secrets

- `SIGNING_KEY`: base64-encoded Android upload keystore.
- `KEY_STORE_PASSWORD`
- `ALIAS`
- `KEY_PASSWORD`
- `YOUTUBE_API_KEY`
- `N2YO_API_KEY`
- `MAPS_API_KEY`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`: raw Google service-account JSON, not base64 encoded.

The service account must be invited under Google Play Console **Users and permissions**, granted access to ISS Tracker, and allowed to manage testing-track releases. The Google Play Android Developer API must also be enabled for its Google Cloud project.

If Play reports that the package cannot be found, create the Play Console app for `com.restart.spacestationtracker` and complete the first AAB upload manually. Subsequent releases can use `.github/workflows/mobile-release.yml`.

## Release behavior

- A push to `master` or a manual workflow dispatch starts the release.
- `version.properties` supplies both the release name and version code.
- Gradle builds and signs `app-release.aab`.
- GitHub retains the AAB as a workflow artifact.
- The same AAB, R8 mapping file, and release notes are uploaded to closed testing.
- The release is marked `completed`, allowing it to be promoted to production from Play Console without rebuilding it.

Every upload requires a new, monotonically increasing `APP_VERSION_CODE`.
