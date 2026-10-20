#!/usr/bin/env node
// Creates (or recreates) "ISS Tracker CI Ad Hoc", the profile the debug channel uses to sign the
// com.restart.spacestationtracker.ios.dev build for Firebase App Distribution, through the App Store Connect API.
// It includes every enabled iPhone/iPad and every Apple Distribution certificate on the team, so it keeps
// working whichever certificate the IOS_DISTRIBUTION_CERTIFICATE_BASE64 secret holds. Rerun after
// registering a new test device, then update the secret:
//   node tools/ci/asc_profiles.mjs [output dir]
//   base64 -i ~/.config/iss-tracker/ci/ios-adhoc.mobileprovision | gh secret set IOS_ADHOC_PROFILE_BASE64
// Reads APP_STORE_CONNECT_API_KEY_ID and APP_STORE_CONNECT_ISSUER_ID from the environment or
// ~/.config/iss-tracker/ci/ci.env, and the key from ~/.appstoreconnect/private_keys/AuthKey_<id>.p8.
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

const home = process.env.HOME;
const out = process.argv[2] ?? path.join(home, ".config/iss-tracker/ci");
const profileName = "ISS Tracker CI Ad Hoc";
const bundleIdentifier = "com.restart.spacestationtracker.ios.dev";

const envFile = path.join(home, ".config/iss-tracker/ci/ci.env");
const fileEnv = fs.existsSync(envFile)
  ? Object.fromEntries(fs.readFileSync(envFile, "utf8").split("\n").filter((l) => /^\w+=/.test(l))
      .map((l) => [l.slice(0, l.indexOf("=")), l.slice(l.indexOf("=") + 1).trim()]))
  : {};
const keyId = process.env.APP_STORE_CONNECT_API_KEY_ID ?? fileEnv.APP_STORE_CONNECT_API_KEY_ID;
const issuer = process.env.APP_STORE_CONNECT_ISSUER_ID ?? fileEnv.APP_STORE_CONNECT_ISSUER_ID;
if (!keyId || !issuer) {
  console.error(`Set APP_STORE_CONNECT_API_KEY_ID and APP_STORE_CONNECT_ISSUER_ID (environment or ${envFile}).`);
  process.exit(2);
}
const pem = fs.readFileSync(path.join(home, `.appstoreconnect/private_keys/AuthKey_${keyId}.p8`), "utf8");

const b64 = (data) => Buffer.from(data).toString("base64url");
const now = Math.floor(Date.now() / 1000);
const unsigned = `${b64(JSON.stringify({ alg: "ES256", kid: keyId, typ: "JWT" }))}.${b64(JSON.stringify({ iss: issuer, iat: now, exp: now + 900, aud: "appstoreconnect-v1" }))}`;
const token = `${unsigned}.${crypto.sign("sha256", Buffer.from(unsigned), { key: pem, dsaEncoding: "ieee-p1363" }).toString("base64url")}`;

async function api(route, init = {}) {
  const res = await fetch(`https://api.appstoreconnect.apple.com/v1/${route}`,
    { ...init, headers: { authorization: `Bearer ${token}`, "content-type": "application/json", ...init.headers } });
  const text = await res.text();
  if (!res.ok) throw new Error(`${init.method ?? "GET"} ${route}: ${res.status} ${text}`);
  return text ? JSON.parse(text) : {};
}

const certificates = (await api("certificates?filter[certificateType]=DISTRIBUTION,IOS_DISTRIBUTION&limit=200")).data;
if (!certificates.length) throw new Error("No Apple Distribution certificate on this team");
const bundle = (await api(`bundleIds?filter[identifier]=${bundleIdentifier}&filter[platform]=IOS`)).data
  .find((b) => b.attributes.identifier === bundleIdentifier);
if (!bundle) throw new Error(`Bundle ID ${bundleIdentifier} is not registered; add it under Certificates, Identifiers & Profiles first`);
const devices = (await api("devices?filter[status]=ENABLED&limit=200")).data
  .filter((d) => ["IPHONE", "IPAD"].includes(d.attributes.deviceClass));
if (!devices.length) throw new Error("No enabled iPhone or iPad is registered; Ad Hoc builds install only on registered devices");

// Profiles cannot be edited; replace the old one so the new device list or certificate takes effect.
for (const old of (await api(`profiles?filter[name]=${encodeURIComponent(profileName)}`)).data) {
  await api(`profiles/${old.id}`, { method: "DELETE" });
}
const profile = (await api("profiles", { method: "POST", body: JSON.stringify({ data: {
  type: "profiles",
  attributes: { name: profileName, profileType: "IOS_APP_ADHOC" },
  relationships: {
    bundleId: { data: { type: "bundleIds", id: bundle.id } },
    certificates: { data: certificates.map((c) => ({ type: "certificates", id: c.id })) },
    devices: { data: devices.map((d) => ({ type: "devices", id: d.id })) }
  }
} }) })).data;
fs.mkdirSync(out, { recursive: true, mode: 0o700 });
const file = path.join(out, "ios-adhoc.mobileprovision");
fs.writeFileSync(file, Buffer.from(profile.attributes.profileContent, "base64"), { mode: 0o600 });
console.log(`${profileName}: ${profile.attributes.uuid}, expires ${profile.attributes.expirationDate.slice(0, 10)}, ` +
  `${devices.length} device(s), ${certificates.length} certificate(s) -> ${file}`);
