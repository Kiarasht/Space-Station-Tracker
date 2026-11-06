// Generates the editable HTML compositions for the Google Play and App Store listings.
// Run: node marketing/google-play/sources/generate.mjs && marketing/google-play/sources/render.sh
//
// The six phone screenshots are one continuous 6480×1920 canvas (6 × 1080), sliced after rendering, so the background
// and the ground-track curve line up exactly. Every device stays whole inside its own slide; only its bottom may bleed off.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const editable = path.join(here, "..", "editable");
fs.mkdirSync(editable, { recursive: true });

// Real captures from marketing/captures/. Google Play and Galaxy Store use Android captures; the App Store uses iOS ones.
// Card bounds are exact capture pixels, inset 1 px; cr is the card's own corner radius, so nothing around it shows.
// Small pills (the countdown badge) include some of the card around them, so the magnified copy has room to breathe.
const CAPTURES = {
  ios: {
    phone: { // iPhone 17 Pro Max at 3×
      w: 1320, h: 2868,
      telemetry: { sx: 49, sy: 235, sw: 536, sh: 478, cr: 59 },
      hubbleCard: { sx: 49, sy: 235, sw: 536, sh: 478, cr: 59 },
      live: { sx: 887, sy: 247, sw: 384, sh: 118, cr: 58 },
      badge: { sx: 992, sy: 446, sw: 248, sh: 129, cr: 50 },
      chips: { sx: 92, sy: 1540, sw: 1150, sh: 102 },
      reminder: { sx: 181, sy: 982, sw: 958, sh: 988, cr: 83 },
      crewHeader: { sx: 40, sy: 240, sw: 940, sh: 170 },
      astronaut: { sx: 49, sy: 518, sw: 1222, sh: 768, cr: 83 },
    },
    tablet: { // iPad Pro 13"
      w: 2064, h: 2752,
      telemetry: { sx: 33, sy: 97, sw: 357, sh: 318, cr: 39 },
      hubbleCard: { sx: 33, sy: 97, sw: 357, sh: 318, cr: 39 },
      live: { sx: 1776, sy: 105, sw: 248, sh: 78, cr: 38 },
      badge: { sx: 1844, sy: 244, sw: 168, sh: 89, cr: 38 },
      chips: { sx: 64, sy: 1569, sw: 784, sh: 64 },
      reminder: { sx: 473, sy: 1079, sw: 1118, sh: 618, cr: 55 },
      crewHeader: { sx: 22, sy: 86, sw: 620, sh: 138 },
    },
  },
  android: {
    phone: { // Pixel 9 Pro XL profile
      w: 1344, h: 2992,
      telemetry: { sx: 49, sy: 208, sw: 521, sh: 478, cr: 59 },
      hubbleCard: { sx: 49, sy: 208, sw: 521, sh: 478, cr: 59 },
      live: { sx: 921, sy: 220, sw: 374, sh: 118, cr: 58 },
      badge: { sx: 1020, sy: 431, sw: 244, sh: 129, cr: 50 },
      chips: { sx: 92, sy: 1537, sw: 1140, sh: 108 },
      reminder: { sx: 193, sy: 894, sw: 958, sh: 1286, cr: 83 },
      crewHeader: { sx: 40, sy: 200, sw: 860, sh: 170 },
      astronaut: { sx: 49, sy: 468, sw: 1246, sh: 660, cr: 83 },
    },
    tablet: { // Pixel Tablet profile, portrait
      w: 1600, h: 2560,
      telemetry: { sx: 33, sy: 81, sw: 346, sh: 318, cr: 39 },
      hubbleCard: { sx: 33, sy: 81, sw: 346, sh: 318, cr: 39 },
      live: { sx: 1318, sy: 89, sw: 249, sh: 78, cr: 38 },
      badge: { sx: 1383, sy: 228, sw: 165, sh: 89, cr: 38 },
      chips: { sx: 73, sy: 1322, sw: 776, sh: 66 },
      reminder: { sx: 241, sy: 881, sw: 1118, sh: 782, cr: 55 },
      crewHeader: { sx: 30, sy: 72, sw: 570, sh: 116 },
    },
  },
};
let PLATFORM = "android", PHONE = CAPTURES.android.phone, TABLET = CAPTURES.android.tablet;
function usePlatform(platform) {
  PLATFORM = platform; PHONE = CAPTURES[platform].phone; TABLET = CAPTURES[platform].tablet;
}
const shot = (name) => `../../captures/${PLATFORM}/phone/${name}.png`;

// Brand tokens from the app (SpaceStationTheme.kt, the map styles and the icon). See design-brief.md.
const C = {
  ink: "#F7F8FF", mist: "#C9D0F0", muted: "#97A0C8",
  space: "#000020", surface: "#0C1244", gold: "#FFD84D", issRed: "#FF3B30",
  teal: "#2DD4BF", amber: "#FBBF24", coral: "#FF7A7A", issBlue: "#9DBBFF",
  violet: "#8B5CF6", sunset: "#FF9E6B", orchid: "#C26BD6",
};
const font = `"Exo", -apple-system, "SF Pro Display", "Helvetica Neue", Arial, sans-serif`;
const fontFaces = `
  @font-face{font-family:"Exo";src:url(../sources/fonts/exo_variable.ttf) format("truetype");font-weight:100 900}
  @font-face{font-family:"Orbitron";src:url(../sources/fonts/orbitron_variable.ttf) format("truetype");font-weight:400 900}`;

// Taller store formats keep the same design: everything below the copy is scaled by GROW about each slide's
// centre (so magnified cards stay exactly over their originals) and lowered by DY.
let DY = 0, GROW = 1, UNIT = 1080, ANCHOR = 600;
const edgeProblems = [];
/** Records any device or card whose rotated bounds cross the edge of its slide. */
function checkInsideSlide(name, x, y, w, h, rotate) {
  const a = Math.abs(rotate) * Math.PI / 180;
  const half = (w * Math.cos(a) + h * Math.sin(a)) / 2, cx = x + w / 2;
  const left = Math.floor(cx / UNIT) * UNIT;
  if (cx - half < left || cx + half > left + UNIT) {
    edgeProblems.push(`${PLATFORM} ${UNIT}-unit slide ${Math.floor(cx / UNIT) + 1}: ${name} spans ${Math.round(cx - half - left)} to ${Math.round(cx + half - left)}`);
  }
}
function place(x, y, w) {
  const cx = Math.floor(x / UNIT) * UNIT + UNIT / 2;
  return { x: cx + (x - cx) * GROW, y: ANCHOR + (y - ANCHOR) * GROW + DY, w: w * GROW };
}

/**
 * An unbranded Galaxy-style device for the Android sets, around the real capture: a metal rim, slim black bezel,
 * tight corners, a centred camera hole, and volume and power buttons on the right edge. Sizes are fractions of the
 * screen width, except the button positions, which are fractions of the screen height.
 */
function androidDevice(src, { x, y, w, rotate, z }, ratio, f) {
  const h = w * ratio, rim = w * f.rim, bezel = w * f.bezel, radius = w * f.radius, pad = rim + bezel;
  checkInsideSlide(src.split("/").pop(), x, y, w + pad * 2, h + pad * 2, rotate);
  const hole = w * f.camera, holeTop = f.cameraInBezel ? rim + (bezel - hole) / 2 : pad + w * f.cameraY - hole / 2;
  const button = (top, length) => `<span class="side-button" style="top:${pad + h * top}px;height:${w * length}px;
    width:${w * f.buttonWidth}px;left:calc(100% - ${w * f.buttonWidth * 0.25}px)"></span>`;
  return `<div class="phone device" style="left:${x}px;top:${y}px;width:${w + pad * 2}px;height:${h + pad * 2}px;
    transform:rotate(${rotate}deg);z-index:${z};border-radius:${radius + pad}px;padding:${rim}px">
    ${button(f.volumeTop, f.volumeLength)}${button(f.powerTop, f.powerLength)}
    <div class="glass" style="padding:${bezel}px;border-radius:${radius + bezel}px">
      <img src="${src}" style="width:${w}px;height:${h}px;border-radius:${radius}px">
    </div>
    <span class="camera-hole" style="top:${holeTop}px;width:${hole}px;height:${hole}px"></span></div>`;
}

/** A phone: the real capture inside a thin dark bezel, positioned on the canvas. */
function phone(src, { x, y, w, rotate = 0, z = 2 }) {
  ({ x, y, w } = place(x, y, w));
  if (PLATFORM === "android") {
    return androidDevice(src, { x, y, w, rotate, z }, PHONE.h / PHONE.w, {
      rim: 0.006, bezel: 0.012, radius: 0.07, camera: 0.032, cameraY: 0.058, buttonWidth: 0.008,
      volumeTop: 0.19, volumeLength: 0.2, powerTop: 0.31, powerLength: 0.11,
    });
  }
  const h = w * PHONE.h / PHONE.w;
  const bezel = w * 0.022, radius = w * 0.135;
  return `<div class="phone" style="left:${x}px;top:${y}px;width:${w + bezel * 2}px;height:${h + bezel * 2}px;
    transform:rotate(${rotate}deg);z-index:${z};border-radius:${radius + bezel}px;padding:${bezel}px">
    <img src="${src}" style="width:${w}px;height:${h}px;border-radius:${radius}px"></div>`;
}

/**
 * A floating card showing an enlarged region of a capture (authentic UI, just magnified). sx/sy/sw/sh are the
 * card's exact bounds in capture pixels, inset 1 px; cr is its own corner radius, so nothing around it shows.
 */
function crop(src, { sx, sy, sw, sh, x, y, w, rotate = 0, z = 3, radius = 28, cr, glow = "", ring = true, capW = PHONE.w, capH = PHONE.h }) {
  ({ x, y, w } = place(x, y, w));
  const scale = w / sw, h = sh * scale;
  checkInsideSlide(`card from ${src.split("/").pop()}`, x, y, w, h, rotate);
  if (cr !== undefined) radius = cr * scale;
  return `<div class="crop" style="left:${x}px;top:${y}px;width:${w}px;height:${h}px;border-radius:${radius}px;
    transform:rotate(${rotate}deg);z-index:${z};background-image:url(${src});
    background-size:${capW * scale}px ${capH * scale}px;background-position:${-sx * scale}px ${-sy * scale}px;
    ${glow ? `box-shadow:0 34px 70px rgba(0,0,16,.6),${ring ? `0 0 0 2px ${glow},` : ""}0 0 64px ${glow}66;` : ""}"></div>`;
}

// Each device frame's border on each side, as a fraction of the screen width.
const FRAME = { android: { phone: 0.018, tablet: 0.022 }, ios: { phone: 0.022, tablet: 0.018 } };

/**
 * A device on the canvas plus magnified cards lifted out of its screen. Each card is centred over where it sits on
 * the device (allowing for the device's tilt), then nudged by dx/dy, so recaptures never need cards repositioned.
 */
function device(kind, src, { x, y, w, rotate = 0, z = 2 }) {
  const cap = kind === "phone" ? PHONE : TABLET, f = FRAME[PLATFORM][kind];
  const html = (kind === "phone" ? phone : ipad)(src, { x, y, w, rotate, z });
  const width = w * (1 + 2 * f), height = w * cap.h / cap.w + 2 * f * w, s = w / cap.w;
  const card = (box, { from = src, w: cw, dx = 0, dy = 0, ...rest }) => {
    const a = rotate * Math.PI / 180, cx = x + width / 2, cy = y + height / 2;
    const px = x + f * w + (box.sx + box.sw / 2) * s - cx, py = y + f * w + (box.sy + box.sh / 2) * s - cy;
    const ox = cx + px * Math.cos(a) - py * Math.sin(a), oy = cy + px * Math.sin(a) + py * Math.cos(a);
    const ch = cw * box.sh / box.sw;
    const o = { ...box, x: ox - cw / 2 + dx, y: oy - ch / 2 + dy, w: cw, ...rest };
    return kind === "phone" ? crop(from, { z: 4, ...o }) : ipadCrop(from, o);
  };
  return { html, card };
}

function copy(slide, { color = C.gold, lines, accent, sub, top = 118, width = 1080, left = 84, cls = "" }) {
  const title = lines.map((l) => l.replace(accent ?? "\u0000", `<span style="color:${color}">${accent}</span>`)).join("<br>");
  return `<div class="copy ${cls}" style="left:${slide * width + left}px;top:${top}px">
    <h1>${title}</h1>${sub ? `<p>${sub}</p>` : ""}</div>`;
}

const css = `${fontFaces}
  *{box-sizing:border-box;margin:0;padding:0}
  body{background:${C.space};font-family:${font};-webkit-font-smoothing:antialiased;overflow:hidden}
  .canvas{position:relative;overflow:hidden}
  .phone{position:absolute;background:linear-gradient(145deg,#2a3150,#04061a 40%,#161c3a);
    box-shadow:0 60px 120px rgba(0,0,12,.7),0 0 0 2px rgba(255,255,255,.10),inset 0 0 0 1px rgba(255,255,255,.06)}
  .phone img{display:block}
  .phone.device{background:linear-gradient(145deg,#8A8E98,#3A3D45 22%,#191A1F 55%,#2E3037 80%,#6E727C);
    box-shadow:0 60px 120px rgba(0,0,12,.7),0 0 0 1px rgba(0,0,0,.6)}
  .device .glass{width:100%;height:100%;background:#000}
  .side-button{position:absolute;z-index:-1;border-radius:0 3px 3px 0;
    background:linear-gradient(180deg,#70747E,#2A2C33 12%,#3F424A 50%,#2A2C33 88%,#70747E)}
  .camera-hole{position:absolute;left:50%;transform:translateX(-50%);border-radius:50%;
    background:radial-gradient(circle at 38% 36%,#39405A 0%,#10131F 28%,#000 62%);box-shadow:0 0 0 1px rgba(255,255,255,.07)}
  .crop{position:absolute;background-repeat:no-repeat;box-shadow:0 34px 70px rgba(0,0,16,.6),0 0 0 1.5px rgba(255,255,255,.14)}
  .copy{position:absolute;width:920px;z-index:10}
  h1{color:${C.ink};font-size:94px;line-height:1.02;font-weight:800;letter-spacing:-3px}
  .copy p{margin-top:28px;color:${C.mist};font-size:36px;line-height:1.32;font-weight:500;max-width:860px}
  .glow{position:absolute;border-radius:50%;filter:blur(30px)}
`;

/** A deterministic star field, so every render is identical. */
function stars(width, height, count, seed = 7) {
  let s = seed;
  const rand = () => (s = (s * 16807) % 2147483647) / 2147483647;
  let dots = "";
  for (let i = 0; i < count; i++) {
    const r = rand() < 0.1 ? 2.6 : rand() < 0.4 ? 1.8 : 1.1;
    dots += `<circle cx="${(rand() * width).toFixed(0)}" cy="${(rand() * height).toFixed(0)}" r="${r}" fill="#fff" opacity="${(0.25 + rand() * 0.55).toFixed(2)}"/>`;
  }
  return dots;
}

/** An orbit-style ground track: a sine wave across the whole canvas. */
function groundTrack(width, baseY, amp, period, phase = 0) {
  const pts = [];
  for (let x = -100; x <= width + 100; x += 20) {
    pts.push(`${x},${(baseY + amp * Math.sin((x / period) * 2 * Math.PI + phase)).toFixed(1)}`);
  }
  return `M ${pts.join(" L ")}`;
}

// Background: one gradient across all six slides, coloured glows per story beat, a sunrise horizon and
// the sky dome from the app's chart under slides 3–4, and the gold → teal → violet ground track throughout.
const background = (H) => `
  <div style="position:absolute;inset:0;background:linear-gradient(90deg,#000020 0%,#07103A 12%,#08223F 24%,#0E1C52 36%,#24195A 46%,#2C1A55 54%,#171A50 64%,#1A1342 76%,#1D1257 90%,#0B0A35 100%)"></div>
  <svg style="position:absolute;left:0;top:0" width="6480" height="${H}">${stars(6480, H, 520)}</svg>
  <div class="glow" style="left:180px;top:780px;width:1000px;height:1000px;background:radial-gradient(circle,${C.issRed}2e,transparent 65%)"></div>
  <div class="glow" style="left:1350px;top:520px;width:1000px;height:1000px;background:radial-gradient(circle,${C.teal}2a,transparent 65%)"></div>
  <div class="glow" style="left:2500px;top:950px;width:1500px;height:1300px;background:radial-gradient(circle,${C.sunset}3a,transparent 65%)"></div>
  <div class="glow" style="left:3450px;top:700px;width:1100px;height:1100px;background:radial-gradient(circle,${C.amber}24,transparent 65%)"></div>
  <div class="glow" style="left:4500px;top:650px;width:1000px;height:1000px;background:radial-gradient(circle,${C.coral}28,transparent 65%)"></div>
  <div class="glow" style="left:5550px;top:800px;width:1100px;height:1100px;background:radial-gradient(circle,${C.violet}40,transparent 65%)"></div>
  <!-- Sunrise over Earth's limb (as in the app icon), spanning slides 3 and 4. -->
  <div style="position:absolute;left:${3240 - 2600}px;top:${H - 430}px;width:5200px;height:5200px;border-radius:50%;
    background:radial-gradient(circle at 50% 0%,#2B4A9A 0%,#0E1A4A 18%,#050A26 40%);
    box-shadow:0 -10px 40px ${C.sunset}aa,0 -50px 160px ${C.sunset}66,0 -140px 320px ${C.orchid}44"></div>
  <svg style="position:absolute;left:0;top:0" width="6480" height="${H}">
    <path d="M ${3240 - 1000} ${H - 380} A 1000 1000 0 0 1 ${3240 + 1000} ${H - 380}" fill="none"
      stroke="#fff" stroke-opacity=".16" stroke-width="3" stroke-dasharray="14 18"/>
    <defs><linearGradient id="track" x1="0" x2="1">
      <stop offset="0" stop-color="${C.gold}" stop-opacity="0"/><stop offset=".07" stop-color="${C.gold}" stop-opacity=".6"/>
      <stop offset=".3" stop-color="${C.teal}" stop-opacity=".7"/><stop offset=".52" stop-color="${C.sunset}" stop-opacity=".7"/>
      <stop offset=".72" stop-color="${C.coral}" stop-opacity=".6"/><stop offset=".9" stop-color="${C.violet}" stop-opacity=".7"/>
      <stop offset="1" stop-color="${C.violet}" stop-opacity="0"/></linearGradient>
      <filter id="soft"><feGaussianBlur stdDeviation="7"/></filter></defs>
    <path id="trk" d="${groundTrack(6480, 640, 120, 2700, 0.6)}" fill="none" stroke="url(#track)" stroke-width="22"
      stroke-linecap="round" filter="url(#soft)" opacity=".5"/>
    <use href="#trk" stroke="url(#track)" stroke-width="5" fill="none"/>
  </svg>`;

// The six phone slides. Every phone sits whole inside its own slide, close under the copy; only its bottom bleeds off.
function phoneSlides() {
  const map = device("phone", shot("map-iss"), { x: 150, y: 490, w: 780 });
  const passes = device("phone", shot("skypath-hero"), { x: 1080 + 150, y: 520, w: 620, rotate: -6 });
  const list = device("phone", shot("skypath-list"), { x: 2160 + 200, y: 520, w: 660, rotate: 6, z: 3 });
  const alerts = device("phone", shot("settings-alerts"), { x: 3240 + 250, y: 520, w: 620, rotate: -4 });
  const crew = device("phone", shot("crew-top"), { x: 4320 + 250, y: 520, w: 620, rotate: 3, z: 3 });
  const hubble = device("phone", shot("map-hubble"), { x: 5400 + 250, y: 520, w: 660, rotate: 4, z: 3 });
  return `
  ${copy(0, { lines: ["See the ISS", "right now."], accent: "right now.",
    sub: "Live position, speed and orbit path on the map." })}
  ${map.html}
  ${map.card(PHONE.telemetry, { w: 480, dx: -40, rotate: -3, glow: C.issRed, ring: false })}
  ${map.card(PHONE.live, { w: 400, dx: 10, rotate: 3, glow: "#FF6B6B" })}

  ${copy(1, { color: "#5EEAD4", lines: ["Know when to", "look up."], accent: "look up.",
    sub: "Every visible pass from where you are, with a countdown." })}
  ${passes.html}
  ${passes.card(PHONE.badge, { w: 420, dx: 130, dy: 10, rotate: 4, glow: C.gold })}
  ${passes.card(PHONE.chips, { w: 820, dx: 40, dy: 40, rotate: 2, radius: 30, glow: C.teal })}

  ${copy(2, { color: "#FFB38A", lines: ["Find it in", "your sky."], accent: "your sky.",
    sub: "Where it rises, how high it climbs and where it sets." })}
  ${list.html}

  ${copy(3, { color: "#FCD34D", lines: ["Never miss", "a bright pass."], accent: "a bright pass.",
    sub: "Automatic alerts for good passes, or a reminder for any pass." })}
  ${alerts.html}
  ${alerts.card(PHONE.reminder, { from: shot("pass-reminder"), w: 560, dx: -60, dy: 140, rotate: 3, z: 5, glow: C.amber })}

  ${copy(4, { color: "#FF9A9A", lines: ["Meet the crew", "in orbit."], accent: "in orbit.",
    sub: "Everyone in space right now, grouped by spacecraft." })}
  ${crew.html}
  ${crew.card(PHONE.crewHeader, { w: 600, dx: -40, rotate: -2, z: 5, radius: 26 })}
  ${crew.card(PHONE.astronaut, { from: shot("crew-astronauts"), w: 680, dx: 60, dy: 700, rotate: 2.5, glow: C.issBlue })}

  ${copy(5, { color: "#C4B5FD", lines: ["Track Hubble", "and Tiangong too."], accent: "and Tiangong too.",
    sub: "Each with its own live position and orbit path." })}
  ${hubble.html}
  ${hubble.card(PHONE.hubbleCard, { w: 470, dx: -50, rotate: -5, z: 5, glow: C.violet, ring: false })}
`;
}

/** One 6-slide canvas in 1080-unit-wide slides, H units tall, drawn at `scale` output pixels per unit. */
function gallery(platform, H, dy, scale = 1, grow = 1) {
  usePlatform(platform);
  DY = dy; GROW = grow; UNIT = 1080; ANCHOR = 600;
  return `<!doctype html><meta charset="utf-8"><title>ISS Tracker gallery</title><style>${css}</style>
<div style="width:${6480 * scale}px;height:${Math.round(H * scale)}px;overflow:hidden">
<div class="canvas" style="width:6480px;height:${H}px;transform:scale(${scale});transform-origin:0 0">${background(H)}${phoneSlides()}</div></div>`;
}
// Google Play: 1080 × 1920 per slide.
fs.writeFileSync(path.join(editable, "gallery.html"), gallery("android", 1920, 0));
// App Store iPhone 6.9": 1320 × 2868 per slide (same layout, taller; devices sit lower and larger).
fs.writeFileSync(path.join(editable, "gallery-appstore-iphone.html"), gallery("ios", 2868 * 1080 / 1320, 90, 1320 / 1080, 1.1));

// Feature graphic: the brand centre-left, and two whole real screens on the right — the live map from slide 1
// in front of the crew screen from slide 5 — bleeding off the bottom edge.
usePlatform("android");
DY = 0; GROW = 1;
fs.writeFileSync(path.join(editable, "feature-graphic.html"), `<!doctype html><meta charset="utf-8"><title>ISS Tracker feature graphic</title>
<style>${css}
  .fg{position:relative;width:1024px;height:500px;overflow:hidden;background:linear-gradient(115deg,#000020 0%,#0C1244 45%,#2C1A55 100%)}
  .fg .phone{box-shadow:0 30px 70px rgba(0,0,12,.75),0 0 0 1.5px rgba(255,255,255,.12)}
  .fg-copy{position:absolute;left:72px;top:0;height:500px;display:flex;align-items:center;gap:32px;z-index:10}
  .fg-copy img{width:150px;height:150px;border-radius:35px;box-shadow:0 24px 60px rgba(0,0,0,.55),0 0 0 2px rgba(255,216,77,.35)}
  .fg-copy h2{color:${C.ink};font-family:"Orbitron",${font};font-size:50px;font-weight:800;letter-spacing:0;line-height:1}
  .fg-copy .sub{color:${C.gold};font-size:17px;font-weight:700;letter-spacing:2.6px;margin-top:14px}
  .fg-copy .tag{color:${C.mist};font-size:24px;font-weight:500;margin-top:18px}
</style>
<div class="fg"><svg style="position:absolute;inset:0" width="1024" height="500">${stars(1024, 500, 90, 3)}</svg>
  <div class="glow" style="left:640px;top:40px;width:420px;height:420px;background:radial-gradient(circle,${C.issRed}40,transparent 65%)"></div>
  <div class="glow" style="left:560px;top:160px;width:360px;height:360px;background:radial-gradient(circle,${C.coral}30,transparent 65%)"></div>
  ${phone(shot("crew-top"), { x: 668, y: 92, w: 186, rotate: -9, z: 1 })}
  ${phone(shot("map-iss"), { x: 800, y: 44, w: 196, rotate: 7, z: 2 })}
  <div class="fg-copy"><img src="../sources/artwork/icon-512.png"><div>
    <h2>ISS Tracker</h2><div class="sub">LIVE MAP · VISIBLE PASSES · ALERTS</div>
    <div class="tag">See it live. Know when to look up.</div></div></div></div>`);

// Play icon: the app's own 1024 px App Store icon (a raster illustration; there is no vector source), at 512 px.
fs.writeFileSync(path.join(editable, "icon.html"), `<!doctype html><meta charset="utf-8"><style>*{margin:0}img{display:block;width:512px;height:512px}</style>
<img src="../../../iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-AppStore-1024.png">`);

// Review sheet: the six exports side by side, at the gap they appear with on a store listing.
const names = ["01-hero", "02-feature", "03-immersive-start", "04-immersive-continuation", "05-feature-spotlight", "06-finale"];
fs.writeFileSync(path.join(editable, "gallery-preview.html"), `<!doctype html><meta charset="utf-8"><style>${fontFaces}
  *{margin:0;box-sizing:border-box}body{background:#00000F;font-family:${font}}
  .sheet{width:2040px;height:760px;padding:56px 60px;background:linear-gradient(180deg,#000020,#00000F)}
  h3{color:#F7F8FF;font-size:30px;font-weight:700;margin-bottom:30px}h3 span{color:#97A0C8;font-weight:500}
  .row{display:flex;gap:24px}.row img{width:300px;height:533px;border-radius:22px;box-shadow:0 20px 50px rgba(0,0,0,.5)}
</style><div class="sheet"><h3>ISS Tracker · Google Play gallery <span>— 6 screenshots, 1080 × 1920</span></h3>
<div class="row">${names.map((n) => `<img src="../phone/${n}.png">`).join("")}</div></div>`);

console.log(`Wrote HTML sources to ${editable}`);

// ---- Tablets: App Store iPad 13" (2064 × 2752) and Google Play tablet (1440 × 2560) ------------------
// Six slides on one 12384-unit-wide canvas, 2064 units per slide; both tablet sets use the same design, scaled up
// about each slide's centre, on canvases of their store's shape.
const IPAD_W = 2064;
const ipadShot = (name) => `../../captures/${PLATFORM}/tablet/${name}.png`;
function ipad(src, { x, y, w, rotate = 0, z = 2 }) {
  ({ x, y, w } = place(x, y, w));
  if (PLATFORM === "android") {
    return androidDevice(src, { x, y, w, rotate, z }, TABLET.h / TABLET.w, {
      rim: 0.004, bezel: 0.018, radius: 0.028, camera: 0.009, cameraInBezel: true, buttonWidth: 0.005,
      volumeTop: 0.09, volumeLength: 0.11, powerTop: 0.03, powerLength: 0.06,
    });
  }
  const h = w * TABLET.h / TABLET.w, bezel = w * 0.018, radius = w * 0.045;
  return `<div class="phone" style="left:${x}px;top:${y}px;width:${w + bezel * 2}px;height:${h + bezel * 2}px;
    transform:rotate(${rotate}deg);z-index:${z};border-radius:${radius + bezel}px;padding:${bezel}px">
    <img src="${src}" style="width:${w}px;height:${h}px;border-radius:${radius}px"></div>`;
}
const ipadCrop = (src, o) => crop(src, { capW: TABLET.w, capH: TABLET.h, radius: 40, z: 4, ...o })
  .replace(/0 34px 70px/g, "0 50px 100px").replace(/0 0 64px/g, "0 0 90px");
const ipadCopy = (slide, o) => copy(slide, { width: IPAD_W, left: 150, top: 170, cls: "ipadcopy", ...o });
const ipadCss = `${css}
  .ipadcopy{width:1760px}
  .ipadcopy h1{font-size:150px;letter-spacing:-5px}.ipadcopy p{font-size:56px;margin-top:40px;max-width:1700px}`;
const ipadBackground = (H) => `
  <div style="position:absolute;inset:0;background:linear-gradient(90deg,#000020 0%,#07103A 12%,#08223F 26%,#24195A 46%,#2C1A55 56%,#171A50 70%,#1A1342 84%,#0B0A35 100%)"></div>
  <svg style="position:absolute;left:0;top:0" width="12384" height="${H}">${stars(12384, H, 900, 11)}</svg>
  ${[[300, 1300, C.issRed, "2e"], [2500, 900, C.teal, "2a"], [4600, 1500, C.sunset, "3a"], [6900, 1100, C.amber, "24"], [8800, 1100, C.coral, "28"], [10900, 1300, C.violet, "40"]]
    .map(([x, y, c, a]) => `<div class="glow" style="left:${x}px;top:${y}px;width:1700px;height:1700px;background:radial-gradient(circle,${c}${a},transparent 65%)"></div>`).join("")}
  <!-- Sunrise over Earth's limb and the sky dome, spanning slides 3 and 4, as on the phone. -->
  <div style="position:absolute;left:${6192 - 4800}px;top:${H - 720}px;width:9600px;height:9600px;border-radius:50%;
    background:radial-gradient(circle at 50% 0%,#2B4A9A 0%,#0E1A4A 18%,#050A26 40%);
    box-shadow:0 -16px 70px ${C.sunset}aa,0 -90px 280px ${C.sunset}66,0 -240px 560px ${C.orchid}44"></div>
  <svg style="position:absolute;left:0;top:0" width="12384" height="${H}">
    <path d="M ${6192 - 1900} ${H - 640} A 1900 1900 0 0 1 ${6192 + 1900} ${H - 640}" fill="none"
      stroke="#fff" stroke-opacity=".16" stroke-width="5" stroke-dasharray="24 30"/></svg>
  <svg style="position:absolute;left:0;top:0" width="12384" height="${H}" viewBox="0 0 12384 ${H}">
    <defs><linearGradient id="itrack" x1="0" x2="1">
      <stop offset="0" stop-color="${C.gold}" stop-opacity="0"/><stop offset=".07" stop-color="${C.gold}" stop-opacity=".6"/>
      <stop offset=".3" stop-color="${C.teal}" stop-opacity=".7"/><stop offset=".52" stop-color="${C.sunset}" stop-opacity=".7"/>
      <stop offset=".72" stop-color="${C.coral}" stop-opacity=".6"/><stop offset=".9" stop-color="${C.violet}" stop-opacity=".7"/>
      <stop offset="1" stop-color="${C.violet}" stop-opacity="0"/></linearGradient>
      <filter id="isoft"><feGaussianBlur stdDeviation="12"/></filter></defs>
    <path id="itrk" d="${groundTrack(12384, 900, 200, 5000, 0.6)}" fill="none" stroke="url(#itrack)" stroke-width="36"
      stroke-linecap="round" filter="url(#isoft)" opacity=".5"/>
    <use href="#itrk" stroke="url(#itrack)" stroke-width="8" fill="none"/>
  </svg>`;
// The six tablet slides, mirroring the phone set. Each tablet is whole and centred in its own slide, its top at `top`.
function tabletSlides(top) {
  const at = (slide, o) => {
    const w = 1340, x = slide * IPAD_W + IPAD_W / 2 - w * (1 + 2 * FRAME[PLATFORM].tablet) / 2;
    return { x, y: top, w, ...o };
  };
  const map = device("tablet", ipadShot("map"), at(0, {}));
  const passes = device("tablet", ipadShot("passes"), at(1, { rotate: -3 }));
  const list = device("tablet", ipadShot("skypath-list"), at(2, { rotate: 4, z: 3 }));
  const reminder = device("tablet", ipadShot("pass-reminder"), at(3, { rotate: -4 }));
  const crew = device("tablet", ipadShot("crew"), at(4, { rotate: 3, z: 3 }));
  const hubble = device("tablet", ipadShot("map-hubble"), at(5, { rotate: 3 }));
  return `
  ${ipadCopy(0, { lines: ["See the ISS", "right now."], accent: "right now.",
    sub: "Live position, speed and orbit path on the map." })}
  ${map.html}
  ${map.card(TABLET.telemetry, { w: 600, dx: 60, dy: 20, rotate: -3, glow: C.issRed, ring: false })}
  ${map.card(TABLET.live, { w: 400, dx: -60, dy: 20, rotate: 3, glow: "#FF6B6B" })}

  ${ipadCopy(1, { color: "#5EEAD4", lines: ["Know when to", "look up."], accent: "look up.",
    sub: "Every visible pass from where you are, with a countdown." })}
  ${passes.html}
  ${passes.card(TABLET.badge, { w: 540, dx: -130, dy: 30, rotate: 4, glow: C.gold })}
  ${passes.card(TABLET.chips, { w: 1400, dx: 150, dy: 60, rotate: 2, radius: 70, glow: C.teal })}

  ${ipadCopy(2, { color: "#FFB38A", lines: ["Find it in", "your sky."], accent: "your sky.",
    sub: "Where it rises, how high it climbs and where it sets." })}
  ${list.html}

  ${ipadCopy(3, { color: "#FCD34D", lines: ["Never miss", "a bright pass."], accent: "a bright pass.",
    sub: "Automatic alerts for good passes, or a reminder for any pass." })}
  ${reminder.html}
  ${reminder.card(TABLET.reminder, { w: 1300, dy: 160, rotate: -3, z: 5, glow: C.amber })}

  ${ipadCopy(4, { color: "#FF9A9A", lines: ["Meet the crew", "in orbit."], accent: "in orbit.",
    sub: "Everyone in space right now, grouped by spacecraft." })}
  ${crew.html}
  ${crew.card(TABLET.crewHeader, { w: 640, dx: 60, dy: 10, rotate: -2, radius: 44, glow: C.coral })}

  ${ipadCopy(5, { color: "#C4B5FD", lines: ["Track Hubble", "and Tiangong too."], accent: "and Tiangong too.",
    sub: "Each with its own live position and orbit path." })}
  ${hubble.html}
  ${hubble.card(TABLET.hubbleCard, { w: 600, dx: 60, dy: 20, rotate: -5, z: 5, glow: C.violet, ring: false })}
`;
}
/** The six tablet slides in 2064-unit-wide slides, H units tall, drawn at `scale` output pixels per unit. */
function ipadGallery(platform, H, top, scale = 1, grow = 1) {
  usePlatform(platform);
  DY = 0; GROW = grow; UNIT = IPAD_W; ANCHOR = 880;
  return `<!doctype html><meta charset="utf-8"><title>ISS Tracker tablet gallery</title><style>${ipadCss}</style>
<div style="width:${Math.round(12384 * scale)}px;height:${Math.round(H * scale)}px;overflow:hidden">
<div class="canvas" style="width:12384px;height:${H}px;transform:scale(${scale});transform-origin:0 0">${ipadBackground(H)}${tabletSlides(top)}</div></div>`;
}
// App Store iPad 13": 2064 × 2752 per slide. The 4:3 iPad sits higher and slightly smaller so it fits whole.
fs.writeFileSync(path.join(editable, "gallery-appstore-ipad.html"), ipadGallery("ios", 2752, 720, 1, 1.06));
// Google Play tablet (7" and 10" slots): 1440 × 2560 per slide, 9:16.
fs.writeFileSync(path.join(editable, "gallery-play-tablet.html"), ipadGallery("android", 2560 * IPAD_W / 1440, 900, 1440 / IPAD_W, 1.12));
if (edgeProblems.length) {
  console.error(`Layout crosses slide edges:\n  ${edgeProblems.join("\n  ")}`);
  process.exit(1);
}
