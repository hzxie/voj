#!/usr/bin/env bash
#
# Build the Spring Boot artifacts for Verwandlung Online Judge.
#
# Produces two self-contained, executable artifacts:
#   web/target/voj.web.jar       - the web application (embedded Tomcat)
#   judger/target/voj.judger.jar - the judger
#
# Run them with scripts/run-web.sh and scripts/run-judger.sh.
#
# NOTE: The judger compiles a JNI native library via judger/Makefile, which only
# targets Windows and Linux. On macOS the jar still builds but ships without the
# native library, so build/run the judger via Docker (scripts/build-docker.sh)
# or on a Linux host for actual judging.
#
# Usage:
#   scripts/build-jars.sh            # build both artifacts
#   scripts/build-jars.sh web        # build only the web WAR
#   scripts/build-jars.sh judger     # build only the judger JAR
#
# Overridable via environment variables (defaults shown):
#   JDK_HOME=/opt/homebrew/opt/openjdk@25   # Spring 7 supports JDK 17-25
#   SKIP_TESTS=1                            # set to 0 to run tests during build
#
set -euo pipefail

JDK_HOME="${JDK_HOME:-/opt/homebrew/opt/openjdk@25}"
SKIP_TESTS="${SKIP_TESTS:-1}"
TARGET="${1:-all}"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

if [ ! -x "$JDK_HOME/bin/java" ]; then
  echo "ERROR: no JDK at JDK_HOME=$JDK_HOME (install openjdk@25 or override JDK_HOME)." >&2
  exit 1
fi
export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> JDK: $(java -version 2>&1 | head -1)"

MVN_ARGS=()
[ "$SKIP_TESTS" = "1" ] && MVN_ARGS+=("-DskipTests")

case "$TARGET" in
  web)    echo "==> Building web WAR ...";    mvn "${MVN_ARGS[@]}" clean package -pl web ;;
  judger) echo "==> Building judger JAR ..."; mvn "${MVN_ARGS[@]}" clean package -pl judger ;;
  all)    echo "==> Building web WAR + judger JAR ..."; mvn "${MVN_ARGS[@]}" clean package ;;
  *)      echo "Usage: $0 [web|judger|all]" >&2; exit 1 ;;
esac

echo "==> Done. Artifacts:"
ls -lh web/target/voj.web.jar judger/target/voj.judger.jar 2>/dev/null || true
