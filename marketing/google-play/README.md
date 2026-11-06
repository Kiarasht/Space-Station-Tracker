# Google Play listing assets — ISS Tracker

## Uploads
| File | Size | Play Console field |
|---|---|---|
| `icon-512.png` | 512 × 512 | App icon |
| `feature-graphic.png` | 1024 × 500 | Feature graphic |
| `phone/01-hero.png` … `06-finale.png` | 1080 × 1920 each | Phone screenshots, in this order |
| `tablet/01-hero.png` … `06-finale.png` | 1440 × 2560 each (9:16) | 7-inch and 10-inch tablet screenshots (the same files in both slots), in this order |
| `gallery-preview.png` | 2040 × 760 | Review sheet only (do not upload) |

All exports are opaque 24-bit RGB PNGs. See `design-brief.md` for the story, headlines and brand tokens.

## Layout
- `sources/generate.mjs` holds all copy, positions, rotations, crops and colours. It writes the HTML in `editable/`:
  - `gallery.html` is one 6480 × 1920 canvas, so the background and the ground-track curve through all six line up exactly when sliced. Every device sits whole inside its own slide (only its bottom may bleed off), and `generate.mjs` refuses to write a layout where a device or card crosses a slide edge.
  - Magnified cards are placed by `device(...).card(...)`: each is centred over where it sits on that device's screen, allowing for the tilt, then nudged by `dx`/`dy`, so a recapture only needs the card bounds updated.
  - `gallery-play-tablet.html` is the Play tablet set: the iPad design on a 9:16 canvas, with the devices larger and lower.
  - `gallery-appstore-iphone.html` and `gallery-appstore-ipad.html` are the App Store sets (see `marketing/app-store/README.md`).
  - The other files are `feature-graphic.html`, `icon.html` and `gallery-preview.html`.
- `sources/render.sh` renders the HTML with headless Google Chrome at scale factor 1. It slices the canvases (phone, Play tablet, App Store iPhone and iPad), resamples the 6.3″ iPhone set and flattens every output to RGB with `sources/flatten.swift`.
- `marketing/captures/<platform>/<device>/` holds the real captures: Android for the Google Play and Galaxy Store sets, iOS for the App Store sets. `sources/fonts/` holds the app's own Exo and Orbitron fonts.
- `sources/artwork/icon-512.png` is the rendered icon, also used on the feature graphic.

## Regenerate
```sh
node marketing/google-play/sources/generate.mjs
marketing/google-play/sources/render.sh
```
This needs Node, Google Chrome in `/Applications` (or `CHROME=/path/to/chrome`) and Xcode's `swift`. It takes about 30 seconds.

## Recapturing screens
Captures are saved straight into `marketing/captures/`, one file per screen, and a recapture overwrites the old file. After recapturing, re-measure any card that moved and update its bounds in `CAPTURES` in `generate.mjs`.

### Android (Google Play and Galaxy Store)
Galaxy Store rejects listings that show an iPhone or other non-Samsung device, so these sets use only Android captures and an unbranded Galaxy-style frame (`androidDevice` in `generate.mjs`).

1. **Emulators:** use the dedicated `ISS_Marketing_Phone` (Pixel 9 Pro XL profile) and `ISS_Marketing_Tablet` (Pixel Tablet profile) emulators. Start the tablet with `-memory 4096`, because Android kills the app at the default 2 GB, and turn it to portrait:
   ```sh
   adb shell settings put system accelerometer_rotation 0
   adb shell settings put system user_rotation 1
   ```
2. **Clean up:** set dark mode and the System UI demo mode for a clean status bar:
   ```sh
   adb shell cmd uimode night yes
   adb shell settings put global sysui_demo_allowed 1
   adb shell am broadcast -a com.android.systemui.demo -e command enter
   adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
   adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
   adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide
   adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
   adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
   adb shell cmd statusbar send-disable-flag notification-icons
   ```
   The last line hides the icons of the fresh emulator's ongoing system notifications, which demo mode alone leaves showing.
3. **Location:** a simulated `geo fix` doesn't reach the app on a fresh emulator, so use test providers. Use a city with an odd number of upcoming visible passes (five or seven): the next pass sits on top and the later passes fill the tablet's two-column grid in full rows, with no empty cell. Set every device to that city's time zone so pass times are honest. Boston had five for this set:
   ```sh
   adb shell cmd alarm set-timezone America/New_York
   ```
   ```sh
   adb shell appops set com.android.shell android:mock_location allow
   adb shell cmd location set-location-enabled true
   for p in gps network fused; do
     adb shell cmd location providers add-test-provider $p
     adb shell cmd location providers set-test-provider-enabled $p true
     adb shell cmd location providers set-test-provider-location $p --location 42.3601,-71.0589 --accuracy 10
   done
   ```
4. **Launch:** install the debug build with permissions granted, then launch it in capture mode. This debug-only extra hides ads and Remove Ads and turns off Analytics. It marks that emulator's app as ad-free, so never use it on your own device.
   ```sh
   adb install -r -g app/build/outputs/apk/debug/app-debug.apk
   adb shell appops set com.restart.spacestationtracker SCHEDULE_EXACT_ALARM allow
   adb shell dumpsys deviceidle whitelist +com.restart.spacestationtracker
   adb shell am start -n com.restart.spacestationtracker/.MainActivity --ez marketing_capture true
   ```
5. **Screens:** set Map type to Hybrid in Settings, then capture with `marketing/google-play/sources/capture.sh android <phone|tablet> <name> <emulator-serial>`:
   - Phone: `map-iss` (pan down so Antarctica is mostly off screen), `map-hubble` (tap Hubble in the switcher), `skypath-hero`, `skypath-list`, `pass-reminder` (bell on the hero card, with "1 hour before" ticked), `settings-alerts` (Automatic good pass alerts on), `crew-top`, `crew-astronauts`.
   - Tablet: `map`, `map-hubble`, `passes`, `skypath-list`, `pass-reminder`, `crew`.

### iOS (App Store)
1. **Simulators:** use the dedicated "ISS Tracker Marketing" (iPhone 17 Pro Max, iOS 26.3) and "ISS Tracker iPad Marketing" (iPad Pro 13-inch (M5), iOS 26.3) simulators, so your own data is untouched. Set dark appearance, a clean status bar and the same Boston location as Android. Launch the app with `SIMCTL_CHILD_TZ=America/New_York` in front of `xcrun simctl launch`, so it runs on Boston time:
   ```sh
   xcrun simctl ui <udid> appearance dark
   xcrun simctl status_bar <udid> override --time 9:41 --batteryState discharging --batteryLevel 100 --wifiBars 3 --cellularMode notSupported
   xcrun simctl location <udid> set 42.3601,-71.0589
   ```
2. **Launch:** install the debug build and launch it with the debug-only capture arguments. `--hide-ads` keeps test ads out of the captures, `--disable-analytics` turns off Analytics so captures don't count as usage, and `--start-tab` opens `MAP`, `PASSES`, `CREW`, `SETTINGS` or `ABOUT` directly:
   ```sh
   xcrun simctl launch <udid> com.restart.spacestationtracker.ios.dev --hide-ads --disable-analytics --start-tab PASSES
   ```
   These arguments exist only in debug builds (`DebugLaunchArguments` in `iosApp/iosApp/AppModel.swift`); release builds ignore them.
3. **Screens:**
   - Set Map type to Hybrid in Settings first, as on Android. Taps from scripts need a press of about 0.2 s to register, and the reminder dialog opens a second or two after the tap. Wait for the blue location arrow next to the clock to disappear before each capture.
   - `map-iss`: the map at launch, panned down so Antarctica is mostly off screen, with the Live Stream button showing. The button appears only while the live-stream list is under 2 hours old; run the `update-youtube-live-streams` workflow first if it's missing.
   - `map-hubble`: tap Hubble in the switcher.
   - `skypath-hero`, `skypath-list` (scrolled to "Later passes"), `pass-reminder` (bell on the hero card, with "1 hour before" also ticked).
   - `settings-alerts`: Settings with Automatic good pass alerts on and notifications allowed.
   - `crew-top`, `crew-astronauts` (scrolled one screen).
   - iPad, mirroring the phone: `map`, `map-hubble`, `passes`, `skypath-list` (scrolled to "Later passes"), `pass-reminder` (bell on the hero card, "1 hour before" also ticked), `crew`.
4. **Capture:** `marketing/google-play/sources/capture.sh ios <phone|tablet> <name> <udid>` saves the capture to `marketing/captures/ios/`. Nothing in the captures is private; to blur something, run `swift marketing/google-play/sources/redact.swift` on that file in place.

Magnified cards are cropped to the card's exact bounds in capture pixels (`sx, sy, sw, sh`, inset 1 px) with the card's own corner radius (`cr`), so nothing around the card shows.

## Notes and gaps
- **Captured 2026-11-06** in Boston for the Sky Path and Settings screens on both platforms, plus the iOS map and crew screens; the Android map and crew screens date from 2026-11-01. Pass times, countdowns, "Day 100 in space", the crew list and the satellite positions reflect those dates.
- **Real public people:** the current ISS crew appear with their public names, photos and Wikipedia bios, exactly as the app shows them.
- **Icon:** the app icon is a raster illustration with no vector source, so `icon-512.png` is the app's own 1024 px App Store icon resampled, not redrawn.
