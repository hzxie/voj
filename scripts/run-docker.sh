#!/usr/bin/env bash
#
# Build (or pull) and run the Verwandlung Online Judge Docker stack:
#
#   - voj.web    : MariaDB + ActiveMQ + the Spring Boot web app (embedded Tomcat)
#   - voj.judger : the judging worker, reaching voj.web over a shared network
#
# The images are generic: every deployment knob is read from a VOJ_* environment
# variable at runtime (see voj.properties), so this script configures the stack
# with `docker run -e ...` — no rebuild needed to change a password, URL or token.
#
# Zero-config and non-interactive: any secret you do not provide through an
# environment variable is generated as a strong random value and printed once at
# the end.
#
# Usage:
#   scripts/run-docker.sh                          # build, then run
#   VOJ_PULL=1 scripts/run-docker.sh               # run prebuilt Docker Hub images (no build)
#   VOJ_BASE_URL=https://oj.example.edu scripts/run-docker.sh   # run behind a reverse proxy
#   VOJ_JUDGER_API_TOKEN=s3cr3t scripts/run-docker.sh          # pin a secret instead of generating it
#
# Overridable environment variables (defaults shown):
#   WEB_IMAGE=zjhzxhz/voj.web
#   JUDGER_IMAGE=zjhzxhz/voj.judger
#   WEB_PORT=8080
#   NETWORK=voj
#   VOJ_BASE_URL=http://localhost:${WEB_PORT}/voj  # public root (used in e-mails/links)
#   VOJ_JUDGER_API_TOKEN            # shared web<->judger secret; generated if unset
#   VOJ_JMS_BROKER_EMBEDDED=true    # host the broker inside voj.web (saves a JVM, ~150 MB+)
#   VOJ_MAIL_HOST= / VOJ_MAIL_USERNAME= / VOJ_MAIL_PASSWORD=   # empty -> email disabled
#   MYSQL_USER_PASS                 # bundled-DB password (build mode only); generated if unset
#   VOJ_PULL=                       # set to 1 to pull + run published images instead of building
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

WEB_IMAGE="${WEB_IMAGE:-zjhzxhz/voj.web}"
JUDGER_IMAGE="${JUDGER_IMAGE:-zjhzxhz/voj.judger}"
WEB_PORT="${WEB_PORT:-8080}"
NETWORK="${NETWORK:-voj}"
VOJ_BASE_URL="${VOJ_BASE_URL:-http://localhost:${WEB_PORT}/voj}"
VOJ_MAIL_HOST="${VOJ_MAIL_HOST:-}"
VOJ_MAIL_USERNAME="${VOJ_MAIL_USERNAME:-}"
VOJ_MAIL_PASSWORD="${VOJ_MAIL_PASSWORD:-}"
VOJ_JMS_BROKER_EMBEDDED="${VOJ_JMS_BROKER_EMBEDDED:-true}"

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

# The judger API token is read at runtime by both images, so it can always be a
# freshly generated value passed with `-e` (no rebuild, works in pull mode too).
if [ -z "${VOJ_JUDGER_API_TOKEN:-}" ]; then
  VOJ_JUDGER_API_TOKEN="$(gen_secret)"
  GENERATED+=("VOJ_JUDGER_API_TOKEN=${VOJ_JUDGER_API_TOKEN}")
fi

# The bundled MariaDB password is initialised at build time. In build mode we can
# bake a strong random one and tell the app to use it at runtime. In pull mode the
# published image's bundled DB is fixed to "voj", so we must match that.
if [ "${VOJ_PULL:-}" = "1" ]; then
  if [ -n "${MYSQL_USER_PASS:-}" ] && [ "${MYSQL_USER_PASS}" != "voj" ]; then
    echo "WARN: ignoring MYSQL_USER_PASS in pull mode — the published image's bundled" >&2
    echo "      database password is fixed to 'voj'. Build locally to change it." >&2
  fi
  MYSQL_USER_PASS="voj"
else
  if [ -z "${MYSQL_USER_PASS:-}" ]; then
    MYSQL_USER_PASS="$(gen_secret)"
    GENERATED+=("MYSQL_USER_PASS=${MYSQL_USER_PASS}")
  fi
fi

# --------------------------------------------------------------------------- #
# Build (default) or pull the images. Only the bundled-DB password is a build
# arg now; everything else is supplied at run time.
# --------------------------------------------------------------------------- #
if [ "${VOJ_PULL:-}" = "1" ]; then
  echo "==> Pulling published images ..."
  docker pull "${WEB_IMAGE}"
  docker pull "${JUDGER_IMAGE}"
else
  echo "==> Building ${WEB_IMAGE} ..."
  docker build \
    --build-arg "MYSQL_ROOT_PASS=${MYSQL_USER_PASS}" \
    --build-arg "MYSQL_USER_PASS=${MYSQL_USER_PASS}" \
    -t "${WEB_IMAGE}" -f docker/web/Dockerfile .

  echo "==> Building ${JUDGER_IMAGE} ..."
  docker build -t "${JUDGER_IMAGE}" -f docker/judger/Dockerfile .
fi

# --------------------------------------------------------------------------- #
# Run: shared network + the two containers (idempotent — safe to re-run).
# --------------------------------------------------------------------------- #
echo "==> Ensuring network '${NETWORK}' exists ..."
docker network inspect "${NETWORK}" >/dev/null 2>&1 || docker network create "${NETWORK}" >/dev/null

echo "==> Removing any previous voj.web / voj.judger containers ..."
docker rm -f voj.web voj.judger >/dev/null 2>&1 || true

echo "==> Starting voj.web ..."
docker run -d --name voj.web --network "${NETWORK}" -p "${WEB_PORT}:8080" \
  -e "VOJ_DB_PASSWORD=${MYSQL_USER_PASS}" \
  -e "VOJ_BASE_URL=${VOJ_BASE_URL}" \
  -e "VOJ_JUDGER_API_TOKEN=${VOJ_JUDGER_API_TOKEN}" \
  -e "VOJ_JMS_BROKER_EMBEDDED=${VOJ_JMS_BROKER_EMBEDDED}" \
  -e "VOJ_MAIL_HOST=${VOJ_MAIL_HOST}" \
  -e "VOJ_MAIL_USERNAME=${VOJ_MAIL_USERNAME}" \
  -e "VOJ_MAIL_PASSWORD=${VOJ_MAIL_PASSWORD}" \
  "${WEB_IMAGE}" >/dev/null

echo "==> Starting voj.judger ..."
docker run -d --name voj.judger --network "${NETWORK}" \
  -e "VOJ_DB_PASSWORD=${MYSQL_USER_PASS}" \
  -e "VOJ_JUDGER_API_TOKEN=${VOJ_JUDGER_API_TOKEN}" \
  "${JUDGER_IMAGE}" >/dev/null

# --------------------------------------------------------------------------- #
# Summary.
# --------------------------------------------------------------------------- #
echo
echo "==> Verwandlung Online Judge is starting up."
echo "    Web UI: ${VOJ_BASE_URL}"
echo "    (MariaDB + ActiveMQ run inside voj.web; first boot takes a short while.)"
if [ "${#GENERATED[@]}" -gt 0 ]; then
  echo
  echo "    Generated secrets (store them somewhere safe):"
  for kv in "${GENERATED[@]}"; do
    echo "      ${kv}"
  done
fi
echo
echo "    Logs:  docker logs -f voj.web   |   docker logs -f voj.judger"
echo "    Stop:  docker rm -f voj.web voj.judger"
