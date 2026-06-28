#!/usr/bin/env bash
#
# Build (with freshly generated secrets) and run the Verwandlung Online Judge
# Docker stack:
#
#   - voj.web    : MariaDB + ActiveMQ + the Spring Boot web app (embedded Tomcat)
#   - voj.judger : the judging worker, reaching voj.web over a shared network
#
# Zero-config and non-interactive: any secret you do not provide through an
# environment variable is generated as a strong random value and printed once at
# the end. This stays automation-friendly (no prompts that would block CI) while
# never shipping shared, hardcoded credentials.
#
# Usage:
#   scripts/run-docker.sh                       # generate secrets, build, run
#   VOJ_PULL=1 scripts/run-docker.sh            # run prebuilt Docker Hub images (no build)
#   MYSQL_USER_PASS=s3cr3t scripts/run-docker.sh  # pin a secret instead of generating it
#
# Overridable environment variables (defaults shown):
#   WEB_IMAGE=zjhzxhz/voj.web
#   JUDGER_IMAGE=zjhzxhz/voj.judger
#   WEB_PORT=8080
#   NETWORK=voj
#   WEBSITE_URL=localhost:${WEB_PORT}/voj
#   MYSQL_ROOT_PASS                 # generated if unset
#   MYSQL_USER_PASS                 # generated if unset (shared by web + judger)
#   JUDGER_API_TOKEN                # generated if unset (shared by web + judger)
#   MAIL_HOST= / MAIL_USERNAME= / MAIL_PASSWORD=   # empty -> email disabled
#   JMS_BROKER_EMBEDDED=true        # host the broker inside voj.web (saves a JVM,
#                                   # ~150 MB+). Set to false to run a standalone broker.
#   VOJ_PULL=                       # set to 1 to pull + run published images instead of building
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

WEB_IMAGE="${WEB_IMAGE:-zjhzxhz/voj.web}"
JUDGER_IMAGE="${JUDGER_IMAGE:-zjhzxhz/voj.judger}"
WEB_PORT="${WEB_PORT:-8080}"
NETWORK="${NETWORK:-voj}"
WEBSITE_URL="${WEBSITE_URL:-localhost:${WEB_PORT}/voj}"
MAIL_HOST="${MAIL_HOST:-}"
MAIL_USERNAME="${MAIL_USERNAME:-}"
MAIL_PASSWORD="${MAIL_PASSWORD:-}"
JMS_BROKER_EMBEDDED="${JMS_BROKER_EMBEDDED:-true}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is not installed or not on PATH." >&2
  exit 1
fi

# Generate a 128-bit hex secret using whatever is available.
gen_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
  else
    head -c 16 /dev/urandom | od -An -tx1 | tr -d ' \n'
  fi
}

GENERATED=()
if [ -z "${MYSQL_ROOT_PASS:-}" ]; then
  MYSQL_ROOT_PASS="$(gen_secret)"
  GENERATED+=("MYSQL_ROOT_PASS=${MYSQL_ROOT_PASS}")
fi
if [ -z "${MYSQL_USER_PASS:-}" ]; then
  MYSQL_USER_PASS="$(gen_secret)"
  GENERATED+=("MYSQL_USER_PASS=${MYSQL_USER_PASS}")
fi
if [ -z "${JUDGER_API_TOKEN:-}" ]; then
  JUDGER_API_TOKEN="$(gen_secret)"
  GENERATED+=("JUDGER_API_TOKEN=${JUDGER_API_TOKEN}")
fi

# --------------------------------------------------------------------------- #
# Build (default) or pull the images.
# --------------------------------------------------------------------------- #
if [ "${VOJ_PULL:-}" = "1" ]; then
  echo "==> Pulling published images (secrets are whatever was baked at publish time) ..."
  docker pull "${WEB_IMAGE}"
  docker pull "${JUDGER_IMAGE}"
else
  echo "==> Building ${WEB_IMAGE} ..."
  docker build \
    --build-arg "MYSQL_ROOT_PASS=${MYSQL_ROOT_PASS}" \
    --build-arg "MYSQL_USER_PASS=${MYSQL_USER_PASS}" \
    --build-arg "WEBSITE_URL=${WEBSITE_URL}" \
    --build-arg "MAIL_HOST=${MAIL_HOST}" \
    --build-arg "MAIL_USERNAME=${MAIL_USERNAME}" \
    --build-arg "MAIL_PASSWORD=${MAIL_PASSWORD}" \
    --build-arg "JUDGER_API_TOKEN=${JUDGER_API_TOKEN}" \
    --build-arg "JMS_BROKER_EMBEDDED=${JMS_BROKER_EMBEDDED}" \
    -t "${WEB_IMAGE}" -f docker/web/Dockerfile .

  echo "==> Building ${JUDGER_IMAGE} ..."
  docker build \
    --build-arg "MYSQL_USER_PASS=${MYSQL_USER_PASS}" \
    --build-arg "JUDGER_API_TOKEN=${JUDGER_API_TOKEN}" \
    -t "${JUDGER_IMAGE}" -f docker/judger/Dockerfile .
fi

# --------------------------------------------------------------------------- #
# Run: shared network + the two containers (idempotent — safe to re-run).
# --------------------------------------------------------------------------- #
echo "==> Ensuring network '${NETWORK}' exists ..."
docker network inspect "${NETWORK}" >/dev/null 2>&1 || docker network create "${NETWORK}" >/dev/null

echo "==> Removing any previous voj.web / voj.judger containers ..."
docker rm -f voj.web voj.judger >/dev/null 2>&1 || true

echo "==> Starting voj.web ..."
docker run -d --name voj.web --network "${NETWORK}" -p "${WEB_PORT}:8080" "${WEB_IMAGE}" >/dev/null

echo "==> Starting voj.judger ..."
docker run -d --name voj.judger --network "${NETWORK}" "${JUDGER_IMAGE}" >/dev/null

# --------------------------------------------------------------------------- #
# Summary.
# --------------------------------------------------------------------------- #
echo
echo "==> Verwandlung Online Judge is starting up."
echo "    Web UI: http://${WEBSITE_URL}"
echo "    (MariaDB + ActiveMQ run inside voj.web; first boot takes a short while.)"
if [ "${#GENERATED[@]}" -gt 0 ] && [ "${VOJ_PULL:-}" != "1" ]; then
  echo
  echo "    Generated secrets (store them somewhere safe — they are baked into the images):"
  for kv in "${GENERATED[@]}"; do
    echo "      ${kv}"
  done
fi
echo
echo "    Logs:  docker logs -f voj.web   |   docker logs -f voj.judger"
echo "    Stop:  docker rm -f voj.web voj.judger"
