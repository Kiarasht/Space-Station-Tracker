const fs = require("node:fs");
const path = require("node:path");

const outputPath = path.resolve(process.cwd(), "docs/nasa-live-streams.json");
const nasaChannelId = "UCLA_DiR1FfKNvjuUpBHmylQ";
const youtubeApiKey = process.env.YOUTUBE_API_KEY;

async function main() {
  const previous = readPreviousOutput();
  const updatedAt = new Date();

  if (!youtubeApiKey) {
    writeOutput({
      status: "error",
      updatedAt,
      streams: previous.streams,
      error: {
        message: "Missing YOUTUBE_API_KEY secret"
      }
    });
    return;
  }

  try {
    const streams = await fetchNasaLiveStreams();
    writeOutput({
      status: "ok",
      updatedAt,
      streams,
      error: null
    });
  } catch (error) {
    writeOutput({
      status: "error",
      updatedAt,
      streams: previous.streams,
      error: {
        message: error.message
      }
    });
  }
}

async function fetchNasaLiveStreams() {
  const url = new URL("https://www.googleapis.com/youtube/v3/search");
  url.searchParams.set("part", "snippet");
  url.searchParams.set("channelId", nasaChannelId);
  url.searchParams.set("eventType", "live");
  url.searchParams.set("type", "video");
  url.searchParams.set("key", youtubeApiKey);

  const response = await fetch(url);
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`YouTube API ${response.status}: ${body}`);
  }

  const payload = await response.json();
  return (payload.items || [])
    .map((video) => ({
      videoId: video?.id?.videoId,
      title: video?.snippet?.title
    }))
    .filter((stream) => stream.videoId && stream.title);
}

function readPreviousOutput() {
  try {
    return JSON.parse(fs.readFileSync(outputPath, "utf8"));
  } catch {
    return { streams: [] };
  }
}

function writeOutput({ status, updatedAt, streams, error }) {
  const output = {
    status,
    updatedAt: updatedAt.toISOString(),
    updatedAtMillis: updatedAt.getTime(),
    source: "github-actions",
    streams: streams || [],
    error
  };

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`);
}

main();
