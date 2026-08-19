#!/usr/bin/env bash
set -euo pipefail

version_file="${1:-version.properties}"

if [ ! -f "$version_file" ]; then
  echo "Missing app version file: $version_file" >&2
  exit 1
fi

read_property() {
  local key="$1"
  awk -F= -v key="$key" '
    $0 ~ "^[[:space:]]*" key "[[:space:]]*=" {
      value = $2
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      print value
    }
  ' "$version_file" | tail -n 1
}

app_version_name="$(read_property APP_VERSION_NAME)"
app_version_code="$(read_property APP_VERSION_CODE)"

if [ -z "$app_version_name" ] || [ -z "$app_version_code" ]; then
  echo "APP_VERSION_NAME and APP_VERSION_CODE are required in $version_file" >&2
  exit 1
fi

if [ -n "${GITHUB_ENV:-}" ]; then
  {
    echo "APP_VERSION_NAME=$app_version_name"
    echo "APP_VERSION_CODE=$app_version_code"
    echo "ANDROID_VERSION_NAME=$app_version_name"
    echo "ANDROID_VERSION_CODE=$app_version_code"
    echo "IOS_MARKETING_VERSION=$app_version_name"
    echo "IOS_BUILD_NUMBER=$app_version_code"
  } >> "$GITHUB_ENV"
fi

echo "Loaded app version $app_version_name ($app_version_code)"
