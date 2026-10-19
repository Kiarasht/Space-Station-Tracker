#!/bin/bash
# Pre-job hook for the self-hosted runners on the owner's Mac (ACTIONS_RUNNER_HOOK_JOB_STARTED). It runs
# before every job, before any workflow step, and fails the job unless the owner started it.
#
# The workflows have the same checks in their `if:` conditions, but a pull request can edit workflow files;
# it cannot edit this hook, because the runners run an installed copy that lives outside the repository.
# Install or update it with the commands in docs/ci-runners.md after changing this file.
#
# GITHUB_* variables come from GitHub and cannot be overridden by a workflow's env.
set -euo pipefail
owner="Kiarasht"
repository="Kiarasht/Space-Station-Tracker"

refuse() {
  echo "Refusing job on the owner's Mac: $1" >&2
  exit 1
}

[ "${GITHUB_REPOSITORY:-}" = "$repository" ] || refuse "repository ${GITHUB_REPOSITORY:-unknown} is not $repository"
[ "${GITHUB_ACTOR:-}" = "$owner" ] || refuse "actor ${GITHUB_ACTOR:-unknown} is not $owner"
[ "${GITHUB_TRIGGERING_ACTOR:-$GITHUB_ACTOR}" = "$owner" ] || refuse "run was started by ${GITHUB_TRIGGERING_ACTOR}"

case "${GITHUB_EVENT_NAME:-}" in
  push | workflow_dispatch | schedule) ;;
  pull_request)
    # Only the owner's own PRs from branches of this repository; never forks.
    pr_check="$(/usr/bin/python3 -I -c '
import json, sys
event = json.load(open(sys.argv[1]))
pr = event.get("pull_request") or {}
author = (pr.get("user") or {}).get("login", "")
head = ((pr.get("head") or {}).get("repo") or {}).get("full_name", "")
print(author, head)
' "${GITHUB_EVENT_PATH:?}")" || refuse "could not read the pull request event"
    read -r pr_author pr_head <<< "$pr_check"
    [ "$pr_author" = "$owner" ] || refuse "pull request author ${pr_author:-unknown} is not $owner"
    [ "$pr_head" = "$repository" ] || refuse "pull request comes from ${pr_head:-an unknown repository}, not $repository"
    ;;
  *) refuse "event ${GITHUB_EVENT_NAME:-unknown} is not allowed" ;;
esac

echo "Job allowed: ${GITHUB_EVENT_NAME} by ${GITHUB_ACTOR} on ${GITHUB_REPOSITORY}"
