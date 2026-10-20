#!/usr/bin/env bash
# Tags the commit just uploaded as release/<platform>/<APP_VERSION_CODE>. release.yml's gate skips a platform
# whose tag exists, so testing a build on TestFlight from a branch doesn't make the later merge re-upload it.
#   tools/ci/record_release.sh android|ios
set -euo pipefail
platform="${1:?usage: record_release.sh android|ios}"
case "$platform" in android | ios) ;; *) echo "Unknown platform: $platform" >&2; exit 2 ;; esac
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
code="$(awk -F= '$1 ~ /^[[:space:]]*APP_VERSION_CODE[[:space:]]*$/ { gsub(/[[:space:]]/, "", $2); print $2 }' "$root/version.properties" | tail -n 1)"
[[ "$code" =~ ^[0-9]+$ ]] || { echo "APP_VERSION_CODE is missing or not a number" >&2; exit 1; }
tag="release/$platform/$code"
# The runners keep their checkout between runs, so a stale local tag is replaced.
git -C "$root" tag -f "$tag" "${GITHUB_SHA:-HEAD}"
git -C "$root" push origin "refs/tags/$tag"
echo "Recorded $tag"
