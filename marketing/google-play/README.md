# Google Play listing assets — ISS Tracker

## Uploads
| File | Size | Play Console field |
|---|---|---|
| `icon-512.png` | 512 × 512 | App icon |
| `feature-graphic.png` | 1024 × 500 | Feature graphic |
| `01-hero.png` … `06-finale.png` | 1080 × 1920 each | Phone screenshots, in this order |
| `tablet/01-map.png` … `06-finale.png` | 1440 × 2560 each (9:16) | 7-inch and 10-inch tablet screenshots (the same files in both slots), in this order |
| `gallery-preview.png` | 2040 × 760 | Review sheet only (do not upload) |

All exports are opaque 24-bit RGB PNGs. See `design-brief.md` for the story, headlines and brand tokens.

## Layout
- `sources/generate.mjs` holds all copy, positions, rotations, crops and colours. It writes the HTML in `editable/`:
  - `gallery.html` is one 6480 × 1920 canvas, so slides 3 and 4 (and the ground-track curve through all six) line up exactly when sliced.
  - `gallery-play-tablet.html` is the Play tablet set: the iPad design on a 9:16 canvas, with the devices larger and lower.
  - `gallery-appstore-iphone.html` and `gallery-appstore-ipad.html` are the App Store sets (see `marketing/app-store/README.md`).
  - The other files are `feature-graphic.html`, `icon.html` and `gallery-preview.html`.
- `sources/render.sh` renders the HTML with headless Google Chrome at scale factor 1. It slices the canvases (phone, Play tablet, App Store iPhone and iPad), resamples the 6.3″ iPhone set and flattens every output to RGB with `sources/flatten.swift`. It also writes `sources/panorama-full.png` to show how the slides join.
- `sources/screenshots/` holds the captures the compositions use; `sources/fonts/` holds the app's own Exo and Orbitron fonts.
- `sources/artwork/icon-512.png` is the rendered icon, also used on the feature graphic.

## Regenerate
```sh
node marketing/google-play/sources/generate.mjs
marketing/google-play/sources/render.sh
```
This needs Node, Google Chrome in `/Applications` (or `CHROME=/path/to/chrome`) and Xcode's `swift`. It takes about 30 seconds.

## Recapturing screens
1. **Simulators:** use the dedicated "ISS Tracker Marketing" (iPhone 17 Pro Max, iOS 26.3) and "ISS Tracker iPad Marketing" (iPad Pro 13-inch (M5), iOS 26.3) simulators, so your own data is untouched. Set dark appearance, a clean status bar and the London demo location:
   ```sh
   xcrun simctl ui <udid> appearance dark
   xcrun simctl status_bar <udid> override --time 9:41 --batteryState charged --batteryLevel 100
   xcrun simctl location <udid> set 51.5072,-0.1276
   ```
2. **Launch:** install the debug build and launch it with the debug-only capture arguments. `--hide-ads` keeps test ads out of the captures, `--disable-analytics` turns off Analytics so captures don't count as usage, and `--start-tab` opens `MAP`, `PASSES`, `CREW`, `SETTINGS` or `ABOUT` directly:
   ```sh
   xcrun simctl launch <udid> com.restart.spacestationtracker.ios.dev --hide-ads --disable-analytics --start-tab PASSES
   ```
   These arguments exist only in debug builds (`DebugLaunchArguments` in `iosApp/iosApp/AppModel.swift`); release builds ignore them.
3. **Screens:**
   - `map-iss`: the map at launch, with the ISS over the Atlantic and the Live Stream button showing. The button appears only while the live-stream list is under 2 hours old; run the `update-youtube-live-streams` workflow first if it's missing.
   - `map-hubble`: pan the map to Hubble and tap it.
   - `skypath-hero`, `skypath-list` (scrolled to "Later passes"), `pass-reminder` (bell on the hero card, with "1 hour before" also ticked).
   - `settings-alerts`: Settings with Automatic good pass alerts on and notifications allowed.
   - `crew-top`, `crew-astronauts` (scrolled one screen), `about`.
   - iPad: `ipad-map`, `ipad-map-hubble`, `ipad-passes`, `ipad-crew`, `ipad-settings`, `ipad-about`.
4. **Capture:** `marketing/google-play/sources/capture.sh <name> <udid>` saves the raw capture to `artifacts/marketing-raw/`, which is untracked.
5. **Copy:** `marketing/google-play/sources/redact.sh` copies the captures into `sources/screenshots/`. Nothing in them is private, so nothing is blurred; the script can still blur a rectangle if ever needed.

Magnified cards are cropped to the card's exact bounds in capture pixels (`sx, sy, sw, sh`, inset 1 px) with the card's own corner radius (`cr`), so nothing around the card shows. After recapturing, re-measure any card that moved and update those values in `generate.mjs`.

## Notes and gaps
- **Captured on iOS:** captures come from the iOS simulators. The Android app renders the same shared Compose screens; using iPhone and iPad captures for Play is the owner's choice. The status bar is iOS-styled.
- **Captured 2026-10-22/23.** Pass times, countdowns, "Day 101 in space", the crew list and the satellite positions reflect that date.
- **Real public people:** the current ISS crew appear with their public names, photos and Wikipedia bios, exactly as the app shows them.
- **Icon:** the app icon is a raster illustration with no vector source, so `icon-512.png` is the app's own 1024 px App Store icon resampled, not redrawn.
