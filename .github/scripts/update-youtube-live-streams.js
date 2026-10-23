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
// Scheduled ("upcoming") streams are published too, with their start times, so the apps can alert users
// before NASA events such as spacewalks and launches. A full search also looks for new ones.
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
      upcoming: previous.upcoming,
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
    let found;
    let fullSearchAt = lastFullSearchAtMillis;
    if (needsFullSearch) {
      found = await fullSearch(previous);
      fullSearchAt = updatedAt.getTime();
    } else {
      try {
        found = await checkKnownAndRecentVideos(previous);
      } catch (error) {
        console.warn(`Cheap live check failed, falling back to search: ${error.message}`);
        found = await fullSearch(previous);
        fullSearchAt = updatedAt.getTime();
      }
    }
    const { streams, upcoming } = found;
    console.log(`Live streams: ${streams.map((stream) => `${stream.videoId} (${stream.title})`).join(", ") || "none"}`);
    console.log(`Upcoming: ${upcoming.map((event) => `${event.videoId} at ${new Date(event.scheduledStartMillis).toISOString()} (${event.title})`).join(", ") || "none"}`);

    writeOutput({
      status: "ok",
      updatedAt,
      streams,
      upcoming,
      lastFullSearchAtMillis: fullSearchAt,
      lastSuccessAtMillis: updatedAt.getTime(),
      error: null
    });
  } catch (error) {
    writeOutput({
      status: "error",
      updatedAt,
      streams: previous.streams,
      upcoming: previous.upcoming,
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

/** IDs of the channel's scheduled streams that haven't started yet. */
async function searchNasaUpcomingStreams() {
  const payload = await youtubeGet("search", {
    part: "id",
    channelId: nasaChannelId,
    eventType: "upcoming",
    type: "video",
    maxResults: String(videosPerRequest)
  });
  return (payload.items || []).map((video) => video?.id?.videoId);
}

/** Streams the live search found, plus the known and candidate videos confirmed live or scheduled. */
async function fullSearch(previous) {
  const searched = await searchNasaLiveStreams();
  const candidates = await searchNasaLiveCandidates();
  const upcomingCandidates = await searchNasaUpcomingStreams();
  const { live, upcoming } = await classifyVideos([
    ...knownIds(previous),
    ...candidates,
    ...upcomingCandidates
  ]);
  const byId = new Map([...searched, ...live].map((stream) => [stream.videoId, stream]));
  return { streams: [...byId.values()], upcoming };
}

async function checkKnownAndRecentVideos(previous) {
  const uploads = await youtubeGet("playlistItems", {
    part: "contentDetails",
    playlistId: nasaUploadsPlaylistId,
    maxResults: String(videosPerRequest)
  });
  const { live, upcoming } = await classifyVideos([
    ...knownIds(previous),
    ...(uploads.items || []).map((item) => item?.contentDetails?.videoId)
  ]);
  return { streams: live, upcoming };
}

/** The live and scheduled videos from the last run, so each run re-checks them. */
function knownIds(previous) {
  return [...(previous.streams || []), ...(previous.upcoming || [])].map((stream) => stream.videoId);
}

/**
 * The given videos that are live now, and those scheduled to start later (oldest first); one videos.list unit
 * per 50. A scheduled stream whose start time has passed without going live is left out: NASA often leaves
 * those behind after a change of plan.
 */
async function classifyVideos(ids) {
  const uniqueIds = [...new Set(ids.filter(Boolean))];
  const live = [];
  const upcoming = [];
  const now = Date.now();
  for (let start = 0; start < uniqueIds.length; start += videosPerRequest) {
    const payload = await youtubeGet("videos", {
      part: "snippet,liveStreamingDetails",
      id: uniqueIds.slice(start, start + videosPerRequest).join(",")
    });
    for (const video of payload.items || []) {
      const title = video?.snippet?.title;
      if (!video?.id || !title) continue;
      const state = video.snippet.liveBroadcastContent;
      if (state === "live") {
        live.push({ videoId: video.id, title });
      } else if (state === "upcoming") {
        const scheduledStartMillis = Date.parse(video.liveStreamingDetails?.scheduledStartTime ?? "");
        if (Number.isFinite(scheduledStartMillis) && scheduledStartMillis > now) {
          upcoming.push({ videoId: video.id, title, scheduledStartMillis });
        }
      }
    }
  }
  upcoming.sort((a, b) => a.scheduledStartMillis - b.scheduledStartMillis);
  return { live, upcoming };
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
    return { streams: [], upcoming: [] };
  }
}

// updatedAtMillis changes on every run; lastSuccessAtMillis only when YouTube confirmed the list, so the
// apps can tell how old the streams are even while the check keeps failing.
function writeOutput({ status, updatedAt, streams, upcoming, lastFullSearchAtMillis, lastSuccessAtMillis, error }) {
  const output = {
    status,
    updatedAt: updatedAt.toISOString(),
    updatedAtMillis: updatedAt.getTime(),
    lastSuccessAtMillis: Number(lastSuccessAtMillis) || 0,
    lastFullSearchAtMillis: Number(lastFullSearchAtMillis) || 0,
    source: "github-actions",
    streams: streams || [],
    upcoming: upcoming || [],
    error
  };

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`);
}

main();
