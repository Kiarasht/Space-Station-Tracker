# shellcheck shell=bash disable=SC2034  # Variables here are used by the scripts that source this file.
# Shared settings for the build-and-upload scripts. No secret is stored in the repository. In CI every
# credential arrives as a GitHub secret (environment variable, see .github/workflows); for manual runs the
# scripts fall back to the developer's own keystore.properties, local.properties and ~/.config/iss-tracker/ci.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CI_HOME="${ISS_CI_HOME:-$HOME/.config/iss-tracker/ci}"
OUT="$ROOT/artifacts/ci"
FIREBASE_PROJECT="titanium-gadget-112205"
FIREBASE_ANDROID_APP="1:689771934258:android:5940bdea7ae875001a022b"
# The debug channel ships the ".dev" iOS bundle, which has its own Firebase app.
FIREBASE_IOS_DEBUG_APP="1:689771934258:ios:4ccc747d0f5198b31a022b"
ANDROID_PACKAGE="com.restart.spacestationtracker"
APPLE_TEAM_ID="${APPLE_TEAM_ID:-UT45N57AJ6}"
# ci.env (manual runs only) may set APP_STORE_CONNECT_API_KEY_ID, APP_STORE_CONNECT_ISSUER_ID,
# FIREBASE_APP_DISTRIBUTION_GROUPS and GOOGLE_PLAY_TRACK.
if [ -z "${CI:-}" ] && [ -f "$CI_HOME/ci.env" ]; then . "$CI_HOME/ci.env"; fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
mkdir -p "$OUT"
# Key files decoded from secrets live only for this run, outside the checkout.
SECRETS="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/iss-tracker-secrets.XXXXXX")"
chmod 700 "$SECRETS"
# Scripts append shell commands to ON_EXIT (for example, removing a temporary keychain).
ON_EXIT=""
trap 'eval "$ON_EXIT"; rm -rf "$SECRETS"' EXIT

# put <ENV_VAR> <destination> <fallback file in CI_HOME>: the secret when set; for manual runs, else the local copy.
put() {
  local value="${!1:-}"
  if [ -n "$value" ]; then (umask 077; printf '%s\n' "$value" > "$2")
  elif [ -z "${CI:-}" ] && [ -f "$CI_HOME/$3" ]; then cp "$CI_HOME/$3" "$2"
  else echo "Missing $1${CI:+ GitHub secret} (or $CI_HOME/$3)" >&2; return 1; fi
}
# put_base64 <ENV_VAR> <destination> <fallback file in CI_HOME>: same as put, for base64-encoded secrets.
put_base64() {
  local value="${!1:-}"
  if [ -n "$value" ]; then (umask 077; printf '%s' "$value" | base64 -D > "$2")
  elif [ -z "${CI:-}" ] && [ -f "$CI_HOME/$3" ]; then cp "$CI_HOME/$3" "$2"
  else echo "Missing $1${CI:+ GitHub secret} (or $CI_HOME/$3)" >&2; return 1; fi
}
# write_checkout_file <path> <contents>: a CI-only untracked file in the checkout, deleted when the script exits.
# Refused outside GitHub Actions so a manual run can never replace a developer's own keystore.properties.
write_checkout_file() {
  if [ "${GITHUB_ACTIONS:-}" != "true" ]; then
    echo "Refusing to write $1 outside GitHub Actions" >&2; return 1
  fi
  (umask 077; printf '%s\n' "$2" > "$1")
  ON_EXIT="$ON_EXIT rm -f '$1';"
}
# require_env <VAR>...: fail with a clear message when a CI secret is missing.
require_env() {
  local missing=()
  for name in "$@"; do [ -n "${!name:-}" ] || missing+=("$name"); done
  if [ "${#missing[@]}" -gt 0 ]; then echo "Missing GitHub secrets: ${missing[*]}" >&2; return 1; fi
}
# Google Cloud service account for Firebase App Distribution uploads.
firebase_credentials() {
  put FIREBASE_APP_DISTRIBUTION_SERVICE_ACCOUNT_JSON "$SECRETS/firebase.json" firebase-service-account.json
  export GOOGLE_APPLICATION_CREDENTIALS="$SECRETS/firebase.json"
}
release_notes() { echo "${ISS_RELEASE_NOTES:-$(git -C "$ROOT" log -1 --pretty='%h %s')}"; }
version_code() { grep '^APP_VERSION_CODE=' "$ROOT/version.properties" | cut -d= -f2 | tr -d '[:space:]'; }
version_name() { grep '^APP_VERSION_NAME=' "$ROOT/version.properties" | cut -d= -f2 | tr -d '[:space:]'; }
