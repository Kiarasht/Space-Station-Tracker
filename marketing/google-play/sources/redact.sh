#!/bin/sh
# Copies the untracked raw captures into screenshots/, optionally blurring rectangles first.
# Nothing in the current captures is private: London is a public place, and the crew, their photos
# and bios are the public feed every user sees. To blur something, use: r <name> x,y,w,h ...
# (capture pixels: 1320x2868 for iPhone, 2064x2752 for iPad).
set -e
cd "$(dirname "$0")"
raw="${ISS_RAW_CAPTURES:-../../../artifacts/marketing-raw}"
r() { name="$1"; shift; swift redact.swift "$raw/$name.png" "screenshots/$name.png" "$@"; }
c() { cp "$raw/$1.png" "screenshots/$1.png"; echo "copied screenshots/$1.png"; }
mkdir -p screenshots
for name in map-iss skypath-hero skypath-list pass-reminder settings-alerts crew-top crew-astronauts \
  map-hubble about ipad-map ipad-passes ipad-crew ipad-settings ipad-map-hubble ipad-about; do c "$name"; done
