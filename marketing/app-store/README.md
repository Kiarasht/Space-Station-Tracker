# App Store screenshots — ISS Tracker

| Folder | Size | App Store Connect slot |
|---|---|---|
| `iphone/01-hero.png` … `06-finale.png` | 1320 × 2868 | "iPhone with Dynamic Island (large display)", 6.5″–6.9″ |
| `iphone-6.3/01-hero.png` … `06-finale.png` | 1206 × 2622 | "iPhone with Dynamic Island (medium display)", 6.1″/6.3″ (required); the same slides resampled, because that slot rejects 6.9″ files |
| `ipad/01-map.png` … `06-finale.png` | 2064 × 2752 | iPad 13″ display (Apple scales these down for the smaller iPad sizes) |

All files are opaque RGB PNGs. Upload them in filename order.

- **iPhone set:** the same six slides as Google Play, laid out on a taller canvas, with the devices slightly larger and lower. The crew slide adds a second astronaut card at the bottom.
- **iPad set:** real iPad Pro 13″ (M5) captures of the iPad layouts, with magnified cards from the same captures. The Google Play tablet set (`marketing/google-play/tablet/`) is the same design on a 9:16 canvas:
  1. Map: the ISS live, with its telemetry and the Live Stream button
  2. Sky Path: the full-width sky chart and the two-column later passes
  3. On Duty: the two-column crew grid
  4. Settings: automatic pass alerts and alert health
  5. Map with Hubble selected and Tiangong in view
  6. About

## Regenerate
Everything is generated with the Google Play assets from one script and renderer:
```sh
node marketing/google-play/sources/generate.mjs
marketing/google-play/sources/render.sh
```
The sources live in `marketing/google-play/sources/`; the HTML is in `marketing/google-play/editable/` (`gallery-appstore-iphone.html`, `gallery-appstore-ipad.html`).

## Recapturing screens
See `marketing/google-play/README.md` for the simulators, the London location and the debug-only launch arguments. For the iPad:
```sh
xcrun simctl launch <ipad-udid> com.restart.spacestationtracker.ios.dev --hide-ads --disable-analytics --start-tab CREW
marketing/google-play/sources/capture.sh ipad-crew <ipad-udid>
```
Other tabs: `MAP` (default), `PASSES`, `SETTINGS`, `ABOUT`. Then run `marketing/google-play/sources/redact.sh` to copy the captures into `sources/screenshots/`.

## Gaps
- Captured 2026-10-22/23; pass times, crew and satellite positions date from then.
- A fresh install waits for CelesTrak before Sky Path loads. If CelesTrak is slow or rate-limiting, the screen can sit on its spinner for about a minute before the app falls back to another source.
