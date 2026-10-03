#!/usr/bin/env bash
#
# Print the project version derived from the git tags, for builds to pass to Maven
# as -Drevision=... (the poms default to 0.0.0-SNAPSHOT otherwise).
#
#   HEAD tagged with a release tag (plain semver, e.g. 1.0.1) -> 1.0.1
#   any other commit after the latest release tag 1.0.1       -> 1.0.2-SNAPSHOT
#   no release tag (or not a git checkout)                    -> 0.0.1-SNAPSHOT
#
# Usage:
#   mvn package -Drevision="$(scripts/version.sh)"
set -euo pipefail

cd "$(dirname "$0")/.."

RELEASE_TAG='^[0-9]+\.[0-9]+\.[0-9]+$'

release_tags() {
  git "$@" 2>/dev/null | grep -E "$RELEASE_TAG" | sort -t. -k1,1n -k2,2n -k3,3n || true
}

current=$(release_tags tag --points-at HEAD | tail -n 1)
if [ -n "$current" ]; then
  echo "$current"
  exit 0
fi

latest=$(release_tags tag --merged HEAD | tail -n 1)
IFS=. read -r major minor patch <<< "${latest:-0.0.0}"
echo "${major}.${minor}.$((patch + 1))-SNAPSHOT"
