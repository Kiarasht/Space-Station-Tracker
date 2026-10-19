#!/usr/bin/env bash
# Builds the Android app and optionally uploads it.
#   tools/ci/android.sh debug   [--upload]   assembleDebug APK (test ads, debug key)  -> Firebase App Distribution
#   tools/ci/android.sh release [--upload]   bundleRelease AAB (production, release key) -> Google Play (alpha track)
# The debug channel ships an APK: Firebase installs APKs directly, while its AAB path goes through Play's
# internal app sharing. Store builds are always App Bundles.
# shellcheck source=tools/ci/config.sh
. "$(dirname "$0")/config.sh"
flavor="${1:?debug or release}"; upload="${2:-}"

# API keys reach Gradle as environment variables (app/build.gradle.kts reads them from local.properties or
# the environment). Release builds fail early when MAPS_API_KEY is missing.
if [ -n "${CI:-}" ] && [ "$flavor" = release ]; then
  require_env SIGNING_KEY KEY_STORE_PASSWORD ALIAS KEY_PASSWORD MAPS_API_KEY YOUTUBE_API_KEY
  printf '%s' "$SIGNING_KEY" | base64 -D > "$SECRETS/release.keystore"
  write_checkout_file "$ROOT/keystore.properties" "storeFile=$SECRETS/release.keystore
storePassword=$KEY_STORE_PASSWORD
keyAlias=$ALIAS
keyPassword=$KEY_PASSWORD"
fi

case "$flavor" in
  debug)
    task=assembleDebug
    output="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
    artifact="$OUT/iss-tracker-debug.apk"
    ;;
  release)
    task=bundleRelease
    output="$ROOT/app/build/outputs/bundle/release/app-release.aab"
    artifact="$OUT/iss-tracker-release.aab"
    ;;
  *) echo "Unknown flavor $flavor" >&2; exit 2 ;;
esac
"$ROOT/gradlew" -p "$ROOT" ":app:$task" --console=plain
cp "$output" "$artifact"
echo "Built $artifact ($(version_name) / $(version_code))"
[ "$upload" = "--upload" ] || exit 0

if [ "$flavor" = debug ]; then
  firebase_credentials
  groups="${FIREBASE_APP_DISTRIBUTION_GROUPS:-}"
  firebase appdistribution:distribute "$artifact" --project "$FIREBASE_PROJECT" --app "$FIREBASE_ANDROID_APP" \
    --release-notes "$(release_notes)" ${groups:+--groups "$groups"}
else
  put GOOGLE_PLAY_SERVICE_ACCOUNT_JSON "$SECRETS/play.json" play-service-account.json
  node "$ROOT/tools/ci/play_upload.mjs" "$artifact" "$ANDROID_PACKAGE" "${GOOGLE_PLAY_TRACK:-alpha}" "$SECRETS/play.json" \
    "$ROOT/distribution/whatsnew" "$ROOT/app/build/outputs/mapping/release/mapping.txt"
fi
