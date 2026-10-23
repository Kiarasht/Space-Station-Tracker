# ISS Tracker — store listing design brief

## App
**ISS Tracker** (listed on Google Play as Space Station Tracker) follows the International Space Station live on a map, predicts every visible pass for your location on the device, and draws each one as a sky chart: where it rises, how high it climbs and where it sets. It can alert you before good passes automatically, or remind you before any single pass. It also shows everyone in space right now, grouped by spacecraft, tracks Hubble and China's Tiangong station on the same map, and links to NASA's live ISS streams.

**Audience:** curious skywatchers, families and space fans who want to see the station with their own eyes, without needing to know orbital mechanics. No account, nothing to set up beyond location.

**Selling points, in order:**
1. Live ISS map: position, altitude, speed, sunlight/shadow and the projected orbit path.
2. Sky Path: every visible pass for your location, with a countdown, peak height, duration and brightness.
3. The sky chart: where to look, from horizon to horizon.
4. Pass alerts: automatic alerts for good passes, or a reminder at the lead time you choose.
5. On Duty: the people in space right now, grouped by spacecraft, with bios.
6. More than the ISS: Hubble and Tiangong on the same map, each with its own orbit path.

## Brand system (from the app)
The app doesn't use the Restart Apps / Pgnify tokens; these come from `SpaceStationTheme.kt`, the map styles and the icon.

| Token | Value | Use |
|---|---|---|
| Deep space | `#000020` → `#0C1244` | Base canvas (app background and surface) |
| Hero gradient | `#0C1244` → `#1B2A6B` → `#2F4FB8` | The app's hero cards; slide glows |
| Gold | `#FFD84D` | Primary action, selected tab, time labels; slide 1 and 6 accent |
| ISS red | `#FF3B30` | ISS marker and orbit path; hero glow |
| Sky Path teal → yellow | `#2DD4BF` → `#FBBF24` | The sky chart's pass curve; slides 2–3 |
| Alert amber | `#FBBF24` | Visibility "very bright", alert accent; slide 4 |
| Crew coral | `#FF7A7A` | On Duty hero accent; slide 5 |
| Hubble violet | `#8B5CF6` / `#A78BFA` | Hubble marker, path and card; slide 6 |
| Tiangong teal | `#14B8A6` | Tiangong marker and path |
| Sunset | `#FF9E6B` / `#C26BD6` | The icon's sunrise over Earth; the horizon on slides 3–4 |

Type: the app's own fonts. **Exo** (800, −3 px tracking) for headlines and subtitles; **Orbitron** for the "ISS Tracker" wordmark on the feature graphic, as on the About screen. Slides carry no eyebrow line and no brand mark (owner's choice, 2026-10-23); each headline's accent phrase takes the slide's colour.

## Narrative and layouts
The six slides are one continuous 6480 × 1920 canvas (7920 × 2868 for the App Store iPhone set). The tablet sets (App Store iPad 13″ and Google Play tablet) follow the same story with real iPad captures. An orbit-style ground track, a gold → teal → violet sine curve, runs through all six. The background moves from deep space (map) through teal (Sky Path) into a sunrise horizon (where to look, alerts) and ends in violet (other spacecraft).

| # | Headline | Layout | Real screens |
|---|---|---|---|
| 1 Hero | See the ISS **right now.** | Large upright phone; the telemetry card is magnified with a red glow and breaks out of the frame | Map, ISS over Europe with its orbit path |
| 2 Dynamic | Know when to **look up.** | Phone tilted −6°; the countdown badge and the pass facts row lift out as floating cards | Sky Path, next visible pass from London |
| 3 Immersive A | Find it in **your sky.** | Phone tilted +7°, bleeding off the right edge into slide 4; a glowing sunrise horizon and sky dome begin | Later passes, sky charts |
| 4 Immersive B | Never miss **a bright pass.** | Continues slide 3's phone on the left edge; second phone at −5°; the reminder dialog enlarged in front | Settings › Automatic pass alerts; Schedule notification dialog |
| 5 Spotlight | Meet the crew **in orbit.** | No phone: stacked, rotated real cards (people-in-space header, expedition card, astronaut card) | On Duty |
| 6 Finale | Track Hubble **and Tiangong too.** | Map phone with Hubble selected and Tiangong in view, plus Hubble's floating telemetry card | Map, Hubble selected |

**Feature graphic (1024 × 500):** no device. The icon, the Orbitron wordmark, "LIVE ISS TRACKER & PASS ALERTS" and "See it live. Know when to look up." sit centre-left, over the real sky chart from slide 2, tilted and fading into deep space.

**Icon (512 × 512):** the app's own icon, rendered from `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-AppStore-1024.png`. The icon is a raster illustration (the ISS over a sunrise Earth); there is no vector source, so it is resampled, not redrawn.

## Accuracy guardrails
- Every interface pixel comes from the debug build running on a dedicated iOS simulator. Floating cards are magnified crops of those same captures; nothing is redrawn.
- Demo data: a London location (public place), the live ISS, Hubble and Tiangong positions, and the public crew feed the app shows to everyone. Astronauts appear with their public names, photos and Wikipedia bios, as in the app.
- Copy describes shipped features only. No ratings, download counts, awards or testimonials.
- The Android app shares the same Compose screens, so iOS captures show what Android users see. The status bar is iOS-styled.
