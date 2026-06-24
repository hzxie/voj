#!/usr/bin/env bash
#
# Build Docker images for Verwandlung Online Judge (web + judger).
#
# The Dockerfiles build from this repository as the build context (your local
# working tree), so local changes are included without pushing first.
#
# Usage:
#   scripts/build-docker.sh            # build both images
#   scripts/build-docker.sh web        # build only the web image
#   scripts/build-docker.sh judger     # build only the judger image
#
# Overridable via environment variables (defaults shown):
#   WEB_IMAGE=zjhzxhz/voj.web
#   JUDGER_IMAGE=zjhzxhz/voj.judger
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

WEB_IMAGE="${WEB_IMAGE:-zjhzxhz/voj.web}"
JUDGER_IMAGE="${JUDGER_IMAGE:-zjhzxhz/voj.judger}"
TARGET="${1:-all}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is not installed or not on PATH." >&2
  exit 1
fi

build_web() {
  echo "==> Building web image: ${WEB_IMAGE}"
  docker build -t "${WEB_IMAGE}" -f docker/web/Dockerfile .
}

build_judger() {
  echo "==> Building judger image: ${JUDGER_IMAGE}"
  docker build -t "${JUDGER_IMAGE}" -f docker/judger/Dockerfile .
}

case "$TARGET" in
  web)    build_web ;;
  judger) build_judger ;;
  all)    build_web; build_judger ;;
  *)      echo "Usage: $0 [web|judger|all]" >&2; exit 1 ;;
esac

echo "==> Done. Images:"
docker images | grep -E "voj\.(web|judger)" || true
