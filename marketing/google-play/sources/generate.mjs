// Generates the editable HTML compositions for the Google Play and App Store listings.
// Run: node marketing/google-play/sources/generate.mjs && marketing/google-play/sources/render.sh
//
// The six phone screenshots are one continuous 6480×1920 canvas (6 × 1080), sliced after rendering,
// so the background, the ground-track curve and the phone that crosses from slide 3 into 4 line up exactly.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const editable = path.join(here, "..", "editable");
fs.mkdirSync(editable, { recursive: true });

// Captures are 1320×2868 (iPhone 17 Pro Max at 3×), copied by redact.sh.
const CAP_W = 1320, CAP_H = 2868;
const shot = (name) => `../sources/screenshots/${name}.png`;

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
let DY = 0, GROW = 1, TALL = false, UNIT = 1080, ANCHOR = 600;
function place(x, y, w) {
  const cx = Math.floor(x / UNIT) * UNIT + UNIT / 2;
  return { x: cx + (x - cx) * GROW, y: ANCHOR + (y - ANCHOR) * GROW + DY, w: w * GROW };
}

/** A phone: the real capture inside a thin dark bezel, positioned on the canvas. */
function phone(src, { x, y, w, rotate = 0, z = 2 }) {
  ({ x, y, w } = place(x, y, w));
  const h = w * CAP_H / CAP_W;
  const bezel = w * 0.022, radius = w * 0.135;
  return `<div class="phone" style="left:${x}px;top:${y}px;width:${w + bezel * 2}px;height:${h + bezel * 2}px;
    transform:rotate(${rotate}deg);z-index:${z};border-radius:${radius + bezel}px;padding:${bezel}px">
    <img src="${src}" style="width:${w}px;height:${h}px;border-radius:${radius}px"></div>`;
}

/**
 * A floating card showing an enlarged region of a capture (authentic UI, just magnified). sx/sy/sw/sh are the
 * card's exact bounds in capture pixels, inset 1 px; cr is its own corner radius, so nothing around it shows.
 */
function crop(src, { sx, sy, sw, sh, x, y, w, rotate = 0, z = 3, radius = 28, cr, glow = "", ring = true, capW = CAP_W, capH = CAP_H }) {
  ({ x, y, w } = place(x, y, w));
  const scale = w / sw, h = sh * scale;
  if (cr !== undefined) radius = cr * scale;
  return `<div class="crop" style="left:${x}px;top:${y}px;width:${w}px;height:${h}px;border-radius:${radius}px;
    transform:rotate(${rotate}deg);z-index:${z};background-image:url(${src});
    background-size:${capW * scale}px ${capH * scale}px;background-position:${-sx * scale}px ${-sy * scale}px;
    ${glow ? `box-shadow:0 34px 70px rgba(0,0,16,.6),${ring ? `0 0 0 2px ${glow},` : ""}0 0 64px ${glow}66;` : ""}"></div>`;
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

const slides = () => `
  ${copy(0, { lines: ["See the ISS", "right now."], accent: "right now.",
    sub: "Live position, speed and orbit path on the map." })}
  ${phone(shot("map-iss"), { x: 150, y: 650, w: 780 })}
  ${crop(shot("map-iss"), { sx: 49, sy: 235, sw: 536, sh: 478, cr: 47, x: 62, y: 684, w: 470, rotate: -3, z: 4, glow: C.issRed })}
  ${crop(shot("map-iss"), { ...LIVE, x: 630, y: 805, w: 370, rotate: 3, z: 4, glow: "#FF6B6B" })}

  ${copy(1, { color: "#5EEAD4", lines: ["Know when to", "look up."], accent: "look up.",
    sub: "Every visible pass from where you are, with a countdown." })}
  ${phone(shot("skypath-hero"), { x: 1080 + 46, y: 640, w: 640, rotate: -6 })}
  ${crop(shot("skypath-hero"), { sx: 968, sy: 475, sw: 243, sh: 71, cr: 35, x: 1080 + 660, y: 770, w: 320, rotate: 4, z: 4, glow: C.gold })}
  ${crop(shot("skypath-hero"), { sx: 100, sy: 1546, sw: 880, sh: 192, x: 1080 + 290, y: 1390, w: 700, rotate: 2, z: 4, radius: 30, glow: C.teal })}

  ${copy(2, { color: "#FFB38A", lines: ["Find it in", "your sky."], accent: "your sky.",
    sub: "Where it rises, how high it climbs and where it sets." })}
  ${phone(shot("skypath-list"), { x: 2160 + 420, y: 640, w: 720, rotate: 7, z: 3 })}

  ${copy(3, { color: "#FCD34D", lines: ["Never miss", "a bright pass."], accent: "a bright pass.",
    sub: "Automatic alerts for good passes, or a reminder for any pass." })}
  ${phone(shot("settings-alerts"), { x: 3240 + 330, y: 700, w: 650, rotate: -5 })}
  ${crop(shot("pass-reminder"), { sx: 181, sy: 982, sw: 958, sh: 988, cr: 83, x: 3240 + 210, y: 1250, w: 580, rotate: 3, z: 5, glow: C.amber })}

  ${copy(4, { color: "#FF9A9A", lines: ["Meet the crew", "in orbit."], accent: "in orbit.",
    sub: "Everyone in space right now, grouped by spacecraft." })}
  ${crop(shot("crew-top"), { sx: 40, sy: 240, sw: 940, sh: 170, x: 4320 + 84, y: 590, w: 640, rotate: -2, z: 5, radius: 26 })}
  ${crop(shot("crew-top"), { sx: 49, sy: 590, sw: 1222, sh: 1399, cr: 83, x: 4320 + 70, y: 760, w: 590, rotate: -3, z: 3, glow: C.coral })}
  ${crop(shot("crew-astronauts"), { sx: 49, sy: 931, sw: 1222, sh: 768, cr: 83, x: 4320 + 360, y: 1250, w: 620, rotate: 2.5, z: 4, glow: C.issBlue })}
  ${TALL ? crop(shot("crew-astronauts"), { sx: 49, sy: 1749, sw: 1222, sh: 765, cr: 83, x: 4320 + 110, y: 1960, w: 640, rotate: -2, z: 5 }) : ""}

  ${copy(5, { color: "#C4B5FD", lines: ["Track Hubble", "and Tiangong too."], accent: "and Tiangong too.",
    sub: "Each with its own live position and orbit path." })}
  ${phone(shot("map-hubble"), { x: 5400 + 330, y: 760, w: 680, rotate: 4, z: 3 })}
  ${crop(shot("map-hubble"), { sx: 49, sy: 235, sw: 443, sh: 478, cr: 47, x: 5400 + 220, y: 840, w: 460, rotate: -6, z: 5, glow: C.violet, ring: false })}
`;
// The Live Stream button on the map capture (top right).
const LIVE = { sx: 804, sy: 247, sw: 466, sh: 109, cr: 54 };

/** One 6-slide canvas in 1080-unit-wide slides, H units tall, drawn at `scale` output pixels per unit. */
function gallery(H, dy, scale = 1, grow = 1) {
  DY = dy; GROW = grow; TALL = H > 2000; UNIT = 1080; ANCHOR = 600;
  return `<!doctype html><meta charset="utf-8"><title>ISS Tracker gallery</title><style>${css}</style>
<div style="width:${6480 * scale}px;height:${Math.round(H * scale)}px;overflow:hidden">
<div class="canvas" style="width:6480px;height:${H}px;transform:scale(${scale});transform-origin:0 0">${background(H)}${slides()}</div></div>`;
}
// Google Play: 1080 × 1920 per slide.
fs.writeFileSync(path.join(editable, "gallery.html"), gallery(1920, 0));
// App Store iPhone 6.9": 1320 × 2868 per slide (same layout, taller; devices sit lower and larger).
fs.writeFileSync(path.join(editable, "gallery-appstore-iphone.html"), gallery(2868 * 1080 / 1320, 90, 1320 / 1080, 1.1));

// Feature graphic: the brand centre-left, and two whole real screens on the right — the live map from slide 1
// in front of the crew screen from slide 5 — bleeding off the bottom edge.
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
<div class="row">${names.map((n) => `<img src="../${n}.png">`).join("")}</div></div>`);

console.log(`Wrote HTML sources to ${editable}`);

// ---- Tablets: App Store iPad 13" (2064 × 2752) and Google Play tablet (1440 × 2560) ------------------
// Real iPad Pro 13" captures (2064 × 2752). Six slides on one 12384-unit-wide canvas; the Play tablet set is the
// same design on a taller canvas (Play wants 9:16), with the devices lower and larger.
const IPAD_W = 2064, IPAD_H = 2752;
const ipadShot = (name) => shot(`ipad-${name}`);
function ipad(src, { x, y, w, rotate = 0, z = 2 }) {
  ({ x, y, w } = place(x, y, w));
  const h = w * IPAD_H / IPAD_W, bezel = w * 0.018, radius = w * 0.045;
  return `<div class="phone" style="left:${x}px;top:${y}px;width:${w + bezel * 2}px;height:${h + bezel * 2}px;
    transform:rotate(${rotate}deg);z-index:${z};border-radius:${radius + bezel}px;padding:${bezel}px">
    <img src="${src}" style="width:${w}px;height:${h}px;border-radius:${radius}px"></div>`;
}
const ipadCrop = (src, o) => crop(src, { capW: IPAD_W, capH: IPAD_H, radius: 40, z: 4, ...o })
  .replace(/0 34px 70px/g, "0 50px 100px").replace(/0 0 64px/g, "0 0 90px");
const ipadCopy = (slide, o) => copy(slide, { width: IPAD_W, left: 150, top: 170, cls: "ipadcopy", ...o });
const ipadCss = `${css}
  .ipadcopy{width:1760px}
  .ipadcopy h1{font-size:150px;letter-spacing:-5px}.ipadcopy p{font-size:56px;margin-top:40px;max-width:1700px}`;
const ipadBackground = (H) => `
  <div style="position:absolute;inset:0;background:linear-gradient(90deg,#000020 0%,#07103A 12%,#08223F 26%,#24195A 46%,#2C1A55 56%,#171A50 70%,#1A1342 84%,#0B0A35 100%)"></div>
  <svg style="position:absolute;left:0;top:0" width="12384" height="${H}">${stars(12384, H, 900, 11)}</svg>
  ${[[500, 1300, C.issRed, "2e"], [2600, 900, C.teal, "2a"], [4700, 1200, C.coral, "28"], [6800, 1000, C.amber, "26"], [8900, 1200, C.violet, "40"], [11000, 1300, C.gold, "2a"]]
    .map(([x, y, c, a]) => `<div class="glow" style="left:${x}px;top:${y}px;width:1700px;height:1700px;background:radial-gradient(circle,${c}${a},transparent 65%)"></div>`).join("")}
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
const ipadSlides = () => `
  ${ipadCopy(0, { lines: ["See the ISS", "right now."], accent: "right now.",
    sub: "Live position, speed and orbit path on the map." })}
  ${ipad(ipadShot("map"), { x: 210, y: 980, w: 1640 })}
  ${ipadCrop(ipadShot("map"), { sx: 33, sy: 97, sw: 357, sh: 318, cr: 31, x: 150, y: 900, w: 740, rotate: -3, glow: C.issRed })}
  ${ipadCrop(ipadShot("map"), { sx: 1725, sy: 118, sw: 301, sh: 52, cr: 26, x: 1400, y: 1040, w: 500, rotate: 3, glow: "#FF6B6B" })}

  ${ipadCopy(1, { color: "#5EEAD4", lines: ["Know when to", "look up."], accent: "look up.",
    sub: "Every visible pass from where you are, drawn across your sky." })}
  ${ipad(ipadShot("passes"), { x: IPAD_W + 260, y: 960, w: 1600, rotate: -3 })}
  ${ipadCrop(ipadShot("passes"), { sx: 1832, sy: 265, sw: 159, sh: 47, cr: 23, x: IPAD_W + 1390, y: 910, w: 500, rotate: 4, glow: C.gold })}
  ${ipadCrop(ipadShot("passes"), { sx: 64, sy: 1566, sw: 812, sh: 70, x: IPAD_W + 260, y: 2230, w: 1560, rotate: 2, radius: 70, glow: C.teal })}

  ${ipadCopy(2, { color: "#FF9A9A", lines: ["Meet the crew", "in orbit."], accent: "in orbit.",
    sub: "Everyone in space right now, grouped by spacecraft." })}
  ${ipad(ipadShot("crew"), { x: 2 * IPAD_W + 300, y: 960, w: 1600, rotate: 3, z: 3 })}
  ${ipadCrop(ipadShot("crew"), { sx: 22, sy: 86, sw: 620, sh: 138, x: 2 * IPAD_W + 150, y: 880, w: 1080, rotate: -2, radius: 44, glow: C.coral })}

  ${ipadCopy(3, { color: "#FCD34D", lines: ["Never miss", "a bright pass."], accent: "a bright pass.",
    sub: "Automatic alerts before good passes, at the times you choose." })}
  ${ipad(ipadShot("settings"), { x: 3 * IPAD_W + 380, y: 1000, w: 1520, rotate: -4 })}
  ${ipadCrop(ipadShot("settings"), { sx: 31, sy: 157, sw: 1000, sh: 470, x: 3 * IPAD_W + 160, y: 2050, w: 1180, rotate: 3, z: 5, radius: 50, glow: C.amber })}

  ${ipadCopy(4, { color: "#C4B5FD", lines: ["Track Hubble", "and Tiangong too."], accent: "and Tiangong too.",
    sub: "Each with its own live position and orbit path." })}
  ${ipad(ipadShot("map-hubble"), { x: 4 * IPAD_W + 230, y: 960, w: 1600, rotate: 3 })}
  ${ipadCrop(ipadShot("map-hubble"), { sx: 33, sy: 97, sw: 308, sh: 317, cr: 31, x: 4 * IPAD_W + 150, y: 1000, w: 640, rotate: -5, z: 5, glow: C.violet, ring: false })}

  ${ipadCopy(5, { lines: ["Look up. It’s", "right there."], accent: "right there.",
    sub: "Live map, visible passes, alerts and the crew in one app." })}
  ${ipad(ipadShot("about"), { x: 5 * IPAD_W + 240, y: 1120, w: 1580, rotate: -2 })}
  ${ipadCrop(ipadShot("about"), { sx: 28, sy: 722, sw: 2008, sh: 194, x: 5 * IPAD_W + 164, y: 1690, w: 1760, rotate: -2, radius: 40, glow: C.gold })}
`;
/** The six tablet slides in 2064-unit-wide slides, H units tall, drawn at `scale` output pixels per unit. */
function ipadGallery(H, dy, scale = 1, grow = 1) {
  DY = dy; GROW = grow; UNIT = IPAD_W; ANCHOR = 880;
  return `<!doctype html><meta charset="utf-8"><title>ISS Tracker tablet gallery</title><style>${ipadCss}</style>
<div style="width:${Math.round(12384 * scale)}px;height:${Math.round(H * scale)}px;overflow:hidden">
<div class="canvas" style="width:12384px;height:${H}px;transform:scale(${scale});transform-origin:0 0">${ipadBackground(H)}${ipadSlides()}</div></div>`;
}
// App Store iPad 13": 2064 × 2752 per slide.
fs.writeFileSync(path.join(editable, "gallery-appstore-ipad.html"), ipadGallery(2752, 0));
// Google Play tablet (7" and 10" slots): 1440 × 2560 per slide, 9:16.
fs.writeFileSync(path.join(editable, "gallery-play-tablet.html"), ipadGallery(2560 * IPAD_W / 1440, 330, 1440 / IPAD_W, 1.12));
