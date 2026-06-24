#!/usr/bin/env bash
#
# Run the Verwandlung Online Judge judger.
#
# Launches the self-contained Spring Boot executable JAR. It is a headless
# (non-web) application that listens on ActiveMQ for submissions and judges them,
# so it expects MySQL and ActiveMQ to be reachable (see
# judger/src/main/resources/voj.properties).
#
# Build it first with scripts/build-jars.sh.
#
# NOTE: Judging relies on a JNI native library that only targets Windows and
# Linux; a macOS-built jar will start but cannot judge. Use Docker or a Linux
# host for real judging.
#
# Usage:
#   scripts/run-judger.sh
#   scripts/run-judger.sh --jms.broker.url=tcp://host:61616   # override props
#   JAVA_OPTS="-Xmx512m" scripts/run-judger.sh
#
# Overridable via environment variables (defaults shown):
#   JDK_HOME=/opt/homebrew/opt/openjdk@25   # falls back to `java` on PATH
#   JAVA_OPTS=                              # extra JVM options
#
set -euo pipefail

JDK_HOME="${JDK_HOME:-/opt/homebrew/opt/openjdk@25}"
JAVA_OPTS="${JAVA_OPTS:-}"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

if [ -x "$JDK_HOME/bin/java" ]; then
  JAVA_BIN="$JDK_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_BIN="java"
else
  echo "ERROR: no Java runtime found (set JDK_HOME or put java on PATH)." >&2
  exit 1
fi

JAR="$PROJECT_ROOT/judger/target/voj.judger.jar"
if [ ! -f "$JAR" ]; then
  echo "ERROR: $JAR not found. Build it first: scripts/build-jars.sh judger" >&2
  exit 1
fi

echo "==> Java: $("$JAVA_BIN" -version 2>&1 | head -1)"
echo "==> Starting voj.judger ..."
exec "$JAVA_BIN" ${JAVA_OPTS} -jar "$JAR" "$@"
