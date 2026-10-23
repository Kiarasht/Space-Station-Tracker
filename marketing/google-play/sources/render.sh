#!/bin/sh
# Renders the HTML sources with headless Chrome, then slices and flattens to opaque RGB PNGs.
# Run from anywhere: marketing/google-play/sources/render.sh
set -e
here="$(cd "$(dirname "$0")" && pwd)"
out="$here/.."
tmp="$(mktemp -d)"
chrome="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"

shoot() { # html width height output
  "$chrome" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --default-background-color=00000000 --virtual-time-budget=4000 \
    --window-size="$2,$3" --screenshot="$4" "file://$(printf %s "$out/editable/$1" | sed "s/ /%20/g")" >/dev/null 2>&1
}
flat() { swift "$here/flatten.swift" "$@"; }

shoot icon.html 512 512 "$tmp/icon.png"
mkdir -p "$here/artwork"
flat "$tmp/icon.png" "$here/artwork/icon-512.png"
cp "$here/artwork/icon-512.png" "$out/icon-512.png"

shoot gallery.html 6480 1920 "$tmp/gallery.png"
i=0
for name in 01-hero 02-feature 03-immersive-start 04-immersive-continuation 05-feature-spotlight 06-finale; do
  flat "$tmp/gallery.png" "$out/$name.png" $((i * 1080)) 0 1080 1920
  i=$((i + 1))
done
flat "$tmp/gallery.png" "$here/panorama-full.png"

# App Store iPhone 6.9" set (1320 x 2868), written to marketing/app-store/iphone/.
store="$out/../app-store/iphone"
mkdir -p "$store"
shoot gallery-appstore-iphone.html 7920 2868 "$tmp/appstore.png"
i=0
for name in 01-hero 02-feature 03-immersive-start 04-immersive-continuation 05-feature-spotlight 06-finale; do
  flat "$tmp/appstore.png" "$store/$name.png" $((i * 1320)) 0 1320 2868
  i=$((i + 1))
done

# App Store iPhone 6.3" set (1206 x 2622) for the medium-display slot, which rejects 6.9" files.
# The two sizes differ in aspect by under 0.1%, so a straight resample of the 6.9" slides is exact enough.
medium="$out/../app-store/iphone-6.3"
mkdir -p "$medium"
for f in "$store"/*.png; do sips -z 2622 1206 "$f" --out "$medium/$(basename "$f")" >/dev/null; done

# App Store iPad 13" set (2064 x 2752), written to marketing/app-store/ipad/.
ipadout="$out/../app-store/ipad"
mkdir -p "$ipadout"
shoot gallery-appstore-ipad.html 12384 2752 "$tmp/ipad.png"
i=0
for name in 01-hero 02-feature 03-immersive-start 04-immersive-continuation 05-feature-spotlight 06-finale; do
  flat "$tmp/ipad.png" "$ipadout/$name.png" $((i * 2064)) 0 2064 2752
  i=$((i + 1))
done

# Google Play tablet set (1440 x 2560, 9:16), for both the 7" and 10" tablet slots, written to tablet/.
tablet="$out/tablet"
mkdir -p "$tablet"
shoot gallery-play-tablet.html 8640 2560 "$tmp/tablet.png"
i=0
for name in 01-hero 02-feature 03-immersive-start 04-immersive-continuation 05-feature-spotlight 06-finale; do
  flat "$tmp/tablet.png" "$tablet/$name.png" $((i * 1440)) 0 1440 2560
  i=$((i + 1))
done

shoot feature-graphic.html 1024 500 "$tmp/feature.png"
flat "$tmp/feature.png" "$out/feature-graphic.png"

shoot gallery-preview.html 2040 760 "$tmp/preview.png"
flat "$tmp/preview.png" "$out/gallery-preview.png"
rm -rf "$tmp"
