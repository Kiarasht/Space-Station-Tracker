#!/usr/bin/env node
// Uploads an Android App Bundle to a Google Play track with the Play Developer API. No dependencies.
//   node tools/ci/play_upload.mjs <bundle.aab> <package> <track> <service-account.json> [whatsnew dir] [mapping.txt]
// Release notes come from <whatsnew dir>/whatsnew-<language> files (for example whatsnew-en-US), and the R8
// mapping file is attached so Play Console can deobfuscate crash reports.
// The service account needs "Release apps to testing tracks" (and production, if used) in Play Console.
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

const [aab, pkg, track, keyFile, whatsNewDir, mappingFile] = process.argv.slice(2);
if (!aab || !pkg || !track || !keyFile) {
  console.error("usage: play_upload.mjs <aab> <package> <track> <key.json> [whatsnew dir] [mapping.txt]");
  process.exit(2);
}
if (!aab.endsWith(".aab")) { console.error("Only .aab bundles are uploaded."); process.exit(2); }
if (!fs.existsSync(keyFile)) { console.error(`Missing Play service-account key at ${keyFile}`); process.exit(1); }
const key = JSON.parse(fs.readFileSync(keyFile, "utf8"));

const releaseNotes = whatsNewDir && fs.existsSync(whatsNewDir)
  ? fs.readdirSync(whatsNewDir)
      .filter((name) => name.startsWith("whatsnew-"))
      .map((name) => ({
        language: name.slice("whatsnew-".length),
        text: fs.readFileSync(path.join(whatsNewDir, name), "utf8").trim().slice(0, 500)
      }))
      .filter((note) => note.text)
  : [];

const b64 = (data) => Buffer.from(data).toString("base64url");
async function token() {
  const now = Math.floor(Date.now() / 1000);
  const head = b64(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = b64(JSON.stringify({ iss: key.client_email, scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: key.token_uri, iat: now, exp: now + 3600 }));
  const signature = crypto.createSign("RSA-SHA256").update(`${head}.${claims}`).sign(key.private_key, "base64url");
  const res = await fetch(key.token_uri, { method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${head}.${claims}.${signature}` }) });
  if (!res.ok) throw new Error(`OAuth token: ${res.status} ${await res.text()}`);
  return (await res.json()).access_token;
}

const auth = { authorization: `Bearer ${await token()}` };
const api = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${pkg}`;
const uploadApi = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${pkg}`;
async function call(url, init = {}) {
  const res = await fetch(url, { ...init, headers: { ...auth, ...init.headers } });
  const text = await res.text();
  if (!res.ok) throw Object.assign(new Error(`${init.method ?? "GET"} ${url}: ${res.status} ${text}`), { body: text });
  return text ? JSON.parse(text) : {};
}
const upload = (url, file) => call(url, { method: "POST", headers: { "content-type": "application/octet-stream" },
  body: fs.readFileSync(file), duplex: "half" });
const json = (body) => ({ method: "PUT", headers: { "content-type": "application/json" }, body: JSON.stringify(body) });

async function release(status) {
  const edit = await call(`${api}/edits`, { method: "POST" });
  const bundle = await upload(`${uploadApi}/edits/${edit.id}/bundles?uploadType=media`, aab);
  console.log(`Uploaded versionCode ${bundle.versionCode}`);
  if (mappingFile && fs.existsSync(mappingFile)) {
    await upload(`${uploadApi}/edits/${edit.id}/apks/${bundle.versionCode}/deobfuscationFiles/proguard?uploadType=media`, mappingFile);
    console.log("Uploaded R8 mapping file");
  }
  await call(`${api}/edits/${edit.id}/tracks/${track}`, json({ track, releases: [{ versionCodes: [String(bundle.versionCode)], status,
    ...(releaseNotes.length ? { releaseNotes } : {}) }] }));
  await call(`${api}/edits/${edit.id}:commit`, { method: "POST" });
  console.log(`Released ${bundle.versionCode} to ${track} (${status})`);
}

try {
  await release("completed");
} catch (error) {
  if (!/draft app/i.test(error.body ?? "")) throw error;
  console.log("Play app is still a draft; uploading as a draft release.");
  await release("draft");
}
