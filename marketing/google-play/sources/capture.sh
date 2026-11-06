#!/bin/sh
# Saves a full-resolution capture into marketing/captures/.
# Usage: marketing/google-play/sources/capture.sh <ios|android> <phone|tablet> <name> [simulator-udid|emulator-serial]
set -e
platform="$1"; device="$2"; name="$3"; target="${4:-}"
out="$(dirname "$0")/../../captures/$platform/$device/$name.png"
mkdir -p "$(dirname "$out")"
case "$platform" in
  ios) xcrun simctl io "${target:-booted}" screenshot --type=png "$out" >/dev/null ;;
  android) "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb" ${target:+-s "$target"} exec-out screencap -p > "$out" ;;
  *) echo "Unknown platform $platform" >&2; exit 2 ;;
esac
echo "$out"
