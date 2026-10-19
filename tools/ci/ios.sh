#!/usr/bin/env bash
# Archives and exports a signed .ipa and optionally uploads it.
#   tools/ci/ios.sh debug   [--upload]   Debug config (.dev bundle, test ads), Ad Hoc profile -> Firebase App Distribution
#   tools/ci/ios.sh release [--upload]   Release config (production ads), App Store profile   -> TestFlight
# The Apple Distribution certificate is imported into a temporary keychain (a runner service cannot use the
# login keychain), added to the search list without dropping the user's own keychains, and the app target is
# signed manually through Configuration/CiSigning.xcconfig, so Swift packages are never handed a profile.
# Without a certificate (manual runs only) it falls back to Xcode automatic signing.
# shellcheck source=tools/ci/config.sh
. "$(dirname "$0")/config.sh"
flavor="${1:?debug or release}"; upload="${2:-}"
case "$flavor" in
  debug)
    config=Debug; method=release-testing; bundle_id=com.restart.spacestationtracker.ios.dev
    profile_var=IOS_ADHOC_PROFILE_BASE64; profile_file=ios-adhoc.mobileprovision
    ;;
  release)
    config=Release; method=app-store-connect; bundle_id=com.restart.spacestationtracker.ios
    profile_var=IOS_APP_STORE_PROVISIONING_PROFILE_BASE64; profile_file=ios-appstore.mobileprovision
    ;;
  *) echo "Unknown flavor $flavor" >&2; exit 2 ;;
esac
project="$ROOT/iosApp/ISSTrackerIOS.xcodeproj"
signing_xcconfig="$ROOT/iosApp/Configuration/CiSigning.xcconfig"

# App Store Connect API key: the APP_STORE_CONNECT_API_KEY_BASE64 secret, else the .p8 in ~/.appstoreconnect/private_keys.
auth=()
key_id="${APP_STORE_CONNECT_API_KEY_ID:-}"; issuer_id="${APP_STORE_CONNECT_ISSUER_ID:-}"
if [ -n "$key_id" ] && [ -n "$issuer_id" ]; then
  key="$HOME/.appstoreconnect/private_keys/AuthKey_${key_id}.p8"
  if [ -n "${APP_STORE_CONNECT_API_KEY_BASE64:-}" ]; then
    key="$SECRETS/AuthKey_${key_id}.p8"
    (umask 077; printf '%s' "$APP_STORE_CONNECT_API_KEY_BASE64" | base64 -D > "$key")
  fi
  auth=(-authenticationKeyPath "$key" -authenticationKeyID "$key_id" -authenticationKeyIssuerID "$issuer_id")
elif [ -n "${CI:-}" ]; then
  echo "APP_STORE_CONNECT_API_KEY_ID, APP_STORE_CONNECT_ISSUER_ID and APP_STORE_CONNECT_API_KEY_BASE64 secrets are required in CI" >&2
  exit 1
fi

# Distribution certificate: the IOS_DISTRIBUTION_CERTIFICATE_BASE64 / IOS_DISTRIBUTION_CERTIFICATE_PASSWORD secrets,
# else ~/.config/iss-tracker/ci/apple-distribution.p12 with its password in the keychain (iss-tracker.ios.distribution).
certificate="$SECRETS/distribution.p12"
if [ -n "${IOS_DISTRIBUTION_CERTIFICATE_BASE64:-}" ]; then
  printf '%s' "$IOS_DISTRIBUTION_CERTIFICATE_BASE64" | base64 -D > "$certificate"
  certificate_password="${IOS_DISTRIBUTION_CERTIFICATE_PASSWORD:?IOS_DISTRIBUTION_CERTIFICATE_PASSWORD secret is required with the certificate}"
elif [ -z "${CI:-}" ] && [ -f "$CI_HOME/apple-distribution.p12" ]; then
  cp "$CI_HOME/apple-distribution.p12" "$certificate"
  certificate_password="$(security find-generic-password -s iss-tracker.ios.distribution -a p12 -w)"
elif [ -n "${CI:-}" ]; then
  echo "IOS_DISTRIBUTION_CERTIFICATE_BASE64 and IOS_DISTRIBUTION_CERTIFICATE_PASSWORD secrets are required in CI" >&2
  exit 1
else
  certificate=""
fi

export_signing="<key>signingStyle</key><string>automatic</string>"
if [ -n "$certificate" ]; then
  profile="$SECRETS/$profile_file"
  put_base64 "$profile_var" "$profile" "$profile_file"
  security cms -D -i "$profile" > "$SECRETS/profile.plist"
  profile_name="$(plutil -extract Name raw -o - "$SECRETS/profile.plist")"
  profile_uuid="$(plutil -extract UUID raw -o - "$SECRETS/profile.plist")"
  profile_app_id="$(plutil -extract Entitlements.application-identifier raw -o - "$SECRETS/profile.plist")"
  if [ "$profile_app_id" != "$APPLE_TEAM_ID.$bundle_id" ]; then
    echo "Profile \"$profile_name\" is for $profile_app_id; expected $APPLE_TEAM_ID.$bundle_id" >&2
    exit 1
  fi
  # Install the profile for Xcode; remove it afterwards only if this run added it.
  for dir in "$HOME/Library/Developer/Xcode/UserData/Provisioning Profiles" "$HOME/Library/MobileDevice/Provisioning Profiles"; do
    mkdir -p "$dir"
    if [ ! -f "$dir/$profile_uuid.mobileprovision" ]; then
      cp "$profile" "$dir/$profile_uuid.mobileprovision"; ON_EXIT="$ON_EXIT rm -f '$dir/$profile_uuid.mobileprovision';"
    fi
  done
  # Temporary keychain, added to the search list without dropping the user's own keychains.
  keychain="$SECRETS/signing.keychain-db"; keychain_password="$(openssl rand -hex 32)"
  original=(); while IFS= read -r line; do line="${line//\"/}"; original+=("$(echo "$line" | xargs)"); done < <(security list-keychains -d user)
  security create-keychain -p "$keychain_password" "$keychain"
  ON_EXIT="security list-keychains -d user -s$(printf " '%s'" "${original[@]}"); security delete-keychain '$keychain' || true; $ON_EXIT"
  security set-keychain-settings -lut 21600 "$keychain"
  security unlock-keychain -p "$keychain_password" "$keychain"
  security list-keychains -d user -s "$keychain" "${original[@]}"
  security import "$certificate" -k "$keychain" -P "$certificate_password" -A -T /usr/bin/codesign -T /usr/bin/security >/dev/null
  security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$keychain_password" "$keychain" >/dev/null
  identity="$(security find-identity -v -p codesigning "$keychain" | awk '/Apple Distribution/ { print $2; exit }')"
  if [ -z "$identity" ]; then echo "No Apple Distribution identity in the imported certificate" >&2; exit 1; fi
  write_checkout_file "$signing_xcconfig" "DEVELOPMENT_TEAM = $APPLE_TEAM_ID
CODE_SIGN_STYLE = Manual
CODE_SIGN_IDENTITY = $identity
CODE_SIGN_IDENTITY[sdk=iphoneos*] = $identity
PROVISIONING_PROFILE_SPECIFIER = $profile_name
OTHER_CODE_SIGN_FLAGS = --keychain $keychain"
  export_signing="<key>signingStyle</key><string>manual</string><key>signingCertificate</key><string>$identity</string>
  <key>provisioningProfiles</key><dict><key>$bundle_id</key><string>$profile_name</string></dict>"
  echo "Signing manually with \"$profile_name\""
else
  write_checkout_file "$signing_xcconfig" "DEVELOPMENT_TEAM = $APPLE_TEAM_ID
CODE_SIGN_STYLE = Automatic"
  echo "No distribution certificate found; using Xcode automatic signing"
fi

(cd "$ROOT/iosApp" && xcodegen generate --spec project.yml --quiet)
archive="$OUT/ISSTracker-$flavor.xcarchive"; rm -rf "$archive"
# -quiet keeps warnings and errors, so a signing failure shows its real cause in the CI log.
# The target's pre-build script builds the shared Kotlin framework through Gradle.
xcodebuild archive -quiet -project "$project" -scheme ISSTrackerIOS -configuration "$config" \
  -destination generic/platform=iOS -archivePath "$archive" -allowProvisioningUpdates ${auth[@]+"${auth[@]}"}

destination="export"
if [ "$flavor" = release ] && [ "$upload" = --upload ]; then destination=upload; fi
options="$OUT/ExportOptions-$flavor.plist"
cat > "$options" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>method</key><string>$method</string>
  <key>destination</key><string>$destination</string>
  $export_signing
  <key>teamID</key><string>$APPLE_TEAM_ID</string>
  <key>manageAppVersionAndBuildNumber</key><false/>
  <key>stripSwiftSymbols</key><true/>
  <key>uploadSymbols</key><true/>
</dict></plist>
PLIST
export_dir="$OUT/ipa-$flavor"; rm -rf "$export_dir"
xcodebuild -exportArchive -quiet -archivePath "$archive" -exportOptionsPlist "$options" -exportPath "$export_dir" \
  -allowProvisioningUpdates ${auth[@]+"${auth[@]}"}
if [ "$destination" = upload ]; then
  echo "Uploaded $(version_name) ($(version_code)) to App Store Connect for TestFlight"
else
  echo "Exported $(version_name) ($(version_code), $config) to $export_dir"
fi

if [ "$upload" = --upload ] && [ "$flavor" = debug ]; then
  firebase_credentials
  groups="${FIREBASE_APP_DISTRIBUTION_GROUPS:-}"
  ipa="$(find "$export_dir" -maxdepth 1 -name '*.ipa' -print -quit)"
  firebase appdistribution:distribute "$ipa" --project "$FIREBASE_PROJECT" \
    --app "$FIREBASE_IOS_DEBUG_APP" --release-notes "$(release_notes)" ${groups:+--groups "$groups"}
fi
