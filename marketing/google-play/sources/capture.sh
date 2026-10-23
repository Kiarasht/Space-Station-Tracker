#!/bin/sh
# Saves a full-resolution capture from the dedicated marketing simulator.
# Usage: marketing/google-play/sources/capture.sh <name> [simulator-udid]
set -e
name="$1"; udid="${2:-${ISS_MARKETING_SIMULATOR:-booted}}"
out="$(dirname "$0")/../../../artifacts/marketing-raw/$name.png"
mkdir -p "$(dirname "$out")"
xcrun simctl io "$udid" screenshot --type=png "$out" >/dev/null
echo "$out"
