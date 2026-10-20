# CI on the owner's Mac

The build workflows run on a self-hosted GitHub Actions runner on the owner's Mac, the same setup as the owner's other apps (Pgnify, GSA Auction Finder, NYCJobs). For them:

- Never add GitHub-hosted runners (`ubuntu-*`, `macos-*`, `windows-*`).
- Never add `upload-artifact`, `actions/cache` or other steps that store files on GitHub.
- Never add setup actions that download toolchains; the jobs use the Mac's own JDK, Android SDK, Xcode, Node and Firebase CLI.

The one exception is `update-youtube-live-streams.yml`, which runs on a GitHub-hosted `ubuntu-latest` runner so the live-stream list keeps updating while the Mac is closed. It's a few seconds of Node with no build or artifacts, and hosted runners are free for public repositories.

## Runners

| Runner | Folder | Labels |
|---|---|---|
| `mac` | `~/actions-runners/iss-tracker` | `self-hosted, macOS, ARM64, iss-tracker` |

It is installed as a LaunchAgent (`actions.runner.Kiarasht-Space-Station-Tracker.mac`). It starts at login, runs in the background with no window, and updates itself. A runner takes one job at a time, so a run's Android and iOS jobs build one after the other; that keeps them from competing for the Mac's memory and disk. For parallel builds, register a second runner with the same `iss-tracker` label (see below).

Watch them from a terminal:

```sh
tail -f ~/Library/Logs/actions.runner.Kiarasht-Space-Station-Tracker.mac/stdout.log
```

Check, stop or start them:

```sh
cd ~/actions-runners/iss-tracker && ./svc.sh status
cd ~/actions-runners/iss-tracker && ./svc.sh stop
cd ~/actions-runners/iss-tracker && ./svc.sh start
```

Jobs queue while the Mac is asleep or logged out and start when it is back. The iOS jobs wrap the build in `caffeinate` so the Mac does not sleep mid-archive.

## Only the owner's runs reach the Mac

This repository is public, and a self-hosted runner on a public repository would otherwise let anyone's pull request run code on the Mac. Only runs the owner (`Kiarasht`) starts may run here: their own pull requests from branches of this repository, their pushes, their manual runs and the scheduled cron. Three layers enforce it:

- **Runner hook (the real guarantee):** the runner runs `~/actions-runners/hooks/iss-tracker-job-started.sh` before every job, set through `ACTIONS_RUNNER_HOOK_JOB_STARTED` in its `.env`. It fails the job before any step runs unless the repository, actor, triggering actor and, for pull requests, the PR author and head repository all match the owner. Unlike the workflows, a pull request cannot edit it: the runners use an installed copy outside the repository. Its source is `tools/ci/runner-job-started.sh`.
- **Workflow conditions:** every job's `if:` checks the same thing, so other people's PRs are skipped instead of failing at the hook.
- **Repository setting:** Actions requires approval for runs from every outside contributor ("Require approval for all outside collaborators" under Settings > Actions > General).

The workflows use `pull_request`, never `pull_request_target`. Never approve a workflow run for a pull request you have not read.

After changing `tools/ci/runner-job-started.sh`, reinstall the copy and restart the runner:

```sh
install -m 0555 tools/ci/runner-job-started.sh ~/actions-runners/hooks/iss-tracker-job-started.sh
```

```sh
cd ~/actions-runners/iss-tracker && ./svc.sh stop && ./svc.sh start
```

## Workflows

A debug channel and a release channel, each with one job per platform. iOS uses one App Store Connect app for both: PR builds and release builds all go to its TestFlight.

| Workflow | Trigger | Job | Builds and sends |
|---|---|---|---|
| `ci.yml`: Mobile debug distribution | Owner's PR to `master` from a branch of this repository, or manual | `android-debug` | Android unit tests and lint, then the debug AAB (test ads) → Firebase App Distribution |
| | | `ios-debug` | Shared tests, then the Debug build (test ads, store bundle ID, build `<code>.<run>.<attempt>`) → TestFlight, internal testing only |
| `release.yml`: Release distribution (Play + TestFlight) | Owner's push to `master` that raises `APP_VERSION_CODE`, or manual | `release-gate` | Skips the uploads when the version code did not go up, and any platform whose build was already uploaded |
| | | `android-release` | Android unit tests, then the signed release AAB (production ads) and R8 mapping → Google Play alpha track and Firebase App Distribution |
| | | `ios-release` | Shared tests, then the Release build (production ads, build `<code>`) → TestFlight, ready to attach to a new App Store version |

| `update-youtube-live-streams.yml` | Every 15 minutes, or manual (GitHub-hosted) | `update-live-streams` | Refreshes `docs/nasa-live-streams.json` on the `live-stream-cache` branch. Finds streams through a live-event search, a plain channel search for "live" confirmed with `videos.list` (the live-event search misses the 24/7 ISS feed), and NASA's recent uploads |

Firebase builds go to the group(s) in the `FIREBASE_APP_DISTRIBUTION_GROUPS` secret. Firebase delivers the AABs through Play's internal app sharing, since the Firebase project is linked to the Play app.

PR builds for iOS are marked internal-testing-only (`testFlightInternalTestingOnly`), so they can be installed by internal testers but never submitted for review; only the release build can go into an App Store version. Their build numbers (`55.12.1`) never clash with the release build's (`55`).

Each successful release upload pushes a tag, `release/<platform>/<APP_VERSION_CODE>` (`tools/ci/record_release.sh`), and the gate skips a platform whose tag exists. A manual run of `release.yml` (both platforms or one) retries a failed upload, since a failed upload leaves no tag.

## Scripts

The workflows only call these scripts, which also run by hand:

```sh
tools/ci/android.sh debug|release [--upload]
tools/ci/ios.sh debug|release [--upload]
```

Output goes to the ignored `artifacts/ci/`. On the runners that is inside each runner's `_work` checkout. For example, the latest release AAB for Galaxy Store submission is at `~/actions-runners/iss-tracker*/_work/Space-Station-Tracker/Space-Station-Tracker/artifacts/ci/iss-tracker-release.aab`.

For manual runs, the scripts use your own `keystore.properties`, `local.properties` and `iosApp/Configuration/Local.xcconfig`. Uploads, the iOS certificate and profiles fall back to `~/.config/iss-tracker/ci/`:

- `firebase-service-account.json` and `play-service-account.json`
- `ios-appstore.mobileprovision`
- `apple-distribution.p12`, with its password in the login keychain as service `iss-tracker.ios.distribution`, account `p12`
- `ci.env`, holding `APP_STORE_CONNECT_API_KEY_ID` and `APP_STORE_CONNECT_ISSUER_ID`

In CI the scripts write `keystore.properties` and `Local.xcconfig` into the runner checkout and delete them when they exit. Key files decoded from secrets go to a per-run temporary folder that is also deleted. They refuse to write those files outside GitHub Actions, so a manual run never replaces your own copies.

iOS signing imports the Apple Distribution certificate into a temporary keychain. A runner service cannot use the login keychain, and your own keychain list is restored afterwards. The app target is signed manually through `iosApp/Configuration/CiSigning.xcconfig`, so Swift packages are never given a profile.

## Secrets

| Secret | Used by |
|---|---|
| `MAPS_API_KEY`, `YOUTUBE_API_KEY` | App API keys (Android builds) |
| `SIGNING_KEY`, `KEY_STORE_PASSWORD`, `ALIAS`, `KEY_PASSWORD` | Android release keystore (base64) and its passwords |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Play uploads |
| `FIREBASE_APP_DISTRIBUTION_SERVICE_ACCOUNT_JSON`, `FIREBASE_APP_DISTRIBUTION_GROUPS` | Debug uploads and their tester groups |
| `APP_STORE_CONNECT_API_KEY_BASE64`, `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APPLE_TEAM_ID` | iOS export and TestFlight upload |
| `IOS_DISTRIBUTION_CERTIFICATE_BASE64`, `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD` | Apple Distribution certificate (.p12) |
| `IOS_APP_STORE_PROVISIONING_PROFILE_BASE64` | App Store profile for `com.restart.spacestationtracker.ios` |

`IOS_SIGNING_KEYCHAIN_PASSWORD` and `N2YO_API_KEY` are no longer used. Each run generates a random keychain password, and passes are predicted on the device.

`tools/ci/asc_profiles.mjs` (an Ad Hoc profile for the `.dev` bundle) is no longer used by CI.

## Re-registering a runner

If a runner is removed on GitHub or its folder is lost, re-register it with a fresh token. Tokens expire after an hour.

```sh
cd ~/actions-runners/iss-tracker && ./svc.sh stop && ./svc.sh uninstall
```

```sh
cd ~/actions-runners/iss-tracker && ./config.sh --unattended --url https://github.com/Kiarasht/Space-Station-Tracker --token "$(gh api -X POST repos/Kiarasht/Space-Station-Tracker/actions/runners/registration-token --jq .token)" --name mac --labels iss-tracker --work _work --replace && ./svc.sh install && ./svc.sh start
```

A re-registered runner keeps its `.env`, so the pre-job hook stays set; check that `.env` still contains the `ACTIONS_RUNNER_HOOK_JOB_STARTED` line. To add a second runner for parallel builds, unpack a runner into `~/actions-runners/iss-tracker-2`, register it the same way with `--name mac-2`, and copy the hook line into its `.env`.
