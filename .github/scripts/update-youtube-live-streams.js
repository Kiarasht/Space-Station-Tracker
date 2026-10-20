const fs = require("node:fs");
const path = require("node:path");

const outputPath = path.resolve(process.cwd(), "docs/nasa-live-streams.json");
const nasaChannelId = "UCLA_DiR1FfKNvjuUpBHmylQ";
// Every channel's uploads playlist ID is its channel ID with the "UC" prefix replaced by "UU".
const nasaUploadsPlaylistId = `UU${nasaChannelId.slice(2)}`;
const youtubeApiKey = process.env.YOUTUBE_API_KEY;
const forceFullSearch = process.env.FORCE_FULL_SEARCH === "true";

// search.list costs 100 quota units, while playlistItems.list and videos.list cost 1 each. A search with
// eventType=live misses long-running streams (the 24/7 ISS feed has been live for months and never shows up
// there), so a full search also runs a plain channel search for "live" and confirms each result with
// videos.list. Once a stream is known, the cheap checks keep re-confirming it.
// A full search runs at most every few hours; the runs in between re-check known and recently uploaded
// videos for a few units.
const fullSearchIntervalMillis = 3 * 60 * 60 * 1000;
const videosPerRequest = 50;

async function main() {
  const previous = readPreviousOutput();
  const updatedAt = new Date();

  if (!youtubeApiKey) {
    writeOutput({
      status: "error",
      updatedAt,
      streams: previous.streams,
      lastFullSearchAtMillis: previous.lastFullSearchAtMillis,
      lastSuccessAtMillis: previous.lastSuccessAtMillis,
      error: {
        message: "Missing YOUTUBE_API_KEY secret"
      }
    });
    return;
  }

  const lastFullSearchAtMillis = Number(previous.lastFullSearchAtMillis) || 0;
  const needsFullSearch =
    forceFullSearch ||
    previous.status !== "ok" ||
    updatedAt.getTime() - lastFullSearchAtMillis >= fullSearchIntervalMillis;

  try {
    let streams;
    let fullSearchAt = lastFullSearchAtMillis;
    if (needsFullSearch) {
      streams = await fullSearch(previous.streams);
      fullSearchAt = updatedAt.getTime();
    } else {
      try {
        streams = await checkKnownAndRecentVideos(previous.streams);
      } catch (error) {
        console.warn(`Cheap live check failed, falling back to search: ${error.message}`);
        streams = await fullSearch(previous.streams);
        fullSearchAt = updatedAt.getTime();
      }
    }
    console.log(`Live streams: ${streams.map((stream) => `${stream.videoId} (${stream.title})`).join(", ") || "none"}`);

    writeOutput({
      status: "ok",
      updatedAt,
      streams,
      lastFullSearchAtMillis: fullSearchAt,
      lastSuccessAtMillis: updatedAt.getTime(),
      error: null
    });
  } catch (error) {
    writeOutput({
      status: "error",
      updatedAt,
      streams: previous.streams,
      lastFullSearchAtMillis,
      lastSuccessAtMillis: previous.lastSuccessAtMillis,
      error: {
        message: error.message
      }
    });
  }
}

async function searchNasaLiveStreams() {
  const payload = await youtubeGet("search", {
    part: "snippet",
    channelId: nasaChannelId,
    eventType: "live",
    type: "video"
  });
  return (payload.items || [])
    .map((video) => ({
      videoId: video?.id?.videoId,
      title: video?.snippet?.title
    }))
    .filter((stream) => stream.videoId && stream.title);
}

/** IDs of the channel's videos matching "live", live or not; videos.list sorts out which ones are. */
async function searchNasaLiveCandidates() {
  const payload = await youtubeGet("search", {
    part: "id",
    channelId: nasaChannelId,
    q: "live",
    type: "video",
    maxResults: String(videosPerRequest)
  });
  return (payload.items || []).map((video) => video?.id?.videoId);
}

/** Streams the live search found, plus the known and candidate videos confirmed live. */
async function fullSearch(previousStreams) {
  const searched = await searchNasaLiveStreams();
  const candidates = await searchNasaLiveCandidates();
  const confirmed = await liveVideos([...(previousStreams || []).map((stream) => stream.videoId), ...candidates]);
  const byId = new Map([...searched, ...confirmed].map((stream) => [stream.videoId, stream]));
  return [...byId.values()];
}

async function checkKnownAndRecentVideos(previousStreams) {
  const uploads = await youtubeGet("playlistItems", {
    part: "contentDetails",
    playlistId: nasaUploadsPlaylistId,
    maxResults: String(videosPerRequest)
  });
  return liveVideos([
    ...(previousStreams || []).map((stream) => stream.videoId),
    ...(uploads.items || []).map((item) => item?.contentDetails?.videoId)
  ]);
}

/** The given videos that are live right now, one videos.list unit per 50. */
async function liveVideos(ids) {
  const uniqueIds = [...new Set(ids.filter(Boolean))];
  const streams = [];
  for (let start = 0; start < uniqueIds.length; start += videosPerRequest) {
    const payload = await youtubeGet("videos", {
      part: "snippet",
      id: uniqueIds.slice(start, start + videosPerRequest).join(",")
    });
    for (const video of payload.items || []) {
      if (video?.snippet?.liveBroadcastContent === "live" && video.id && video.snippet.title) {
        streams.push({ videoId: video.id, title: video.snippet.title });
      }
    }
  }
  return streams;
}

async function youtubeGet(resource, params) {
  const url = new URL(`https://www.googleapis.com/youtube/v3/${resource}`);
  for (const [name, value] of Object.entries(params)) {
    url.searchParams.set(name, value);
  }
  url.searchParams.set("key", youtubeApiKey);

  const response = await fetch(url);
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`YouTube API ${resource} ${response.status}: ${body}`);
  }
  return response.json();
}

function readPreviousOutput() {
  try {
    return JSON.parse(fs.readFileSync(outputPath, "utf8"));
  } catch {
    return { streams: [] };
  }
}

// updatedAtMillis changes on every run; lastSuccessAtMillis only when YouTube confirmed the list, so the
// apps can tell how old the streams are even while the check keeps failing.
function writeOutput({ status, updatedAt, streams, lastFullSearchAtMillis, lastSuccessAtMillis, error }) {
  const output = {
    status,
    updatedAt: updatedAt.toISOString(),
    updatedAtMillis: updatedAt.getTime(),
    lastSuccessAtMillis: Number(lastSuccessAtMillis) || 0,
    lastFullSearchAtMillis: Number(lastFullSearchAtMillis) || 0,
    source: "github-actions",
    streams: streams || [],
    error
  };

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`);
}

main();
