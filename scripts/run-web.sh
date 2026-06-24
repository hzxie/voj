#!/usr/bin/env bash
#
# Run the Verwandlung Online Judge web application.
#
# Launches the self-contained Spring Boot executable WAR (embedded Tomcat).
# By default it serves http://localhost:8080/voj and expects MySQL and ActiveMQ
# to be reachable (see web/src/main/resources/voj.properties).
#
# Build it first with scripts/build-jars.sh.
#
# Usage:
#   scripts/run-web.sh
#   scripts/run-web.sh --server.port=9090            # override Boot properties
#   JAVA_OPTS="-Xmx512m" scripts/run-web.sh
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

WAR="$PROJECT_ROOT/web/target/voj.web.war"
if [ ! -f "$WAR" ]; then
  echo "ERROR: $WAR not found. Build it first: scripts/build-jars.sh web" >&2
  exit 1
fi

echo "==> Java: $("$JAVA_BIN" -version 2>&1 | head -1)"
echo "==> Starting voj.web (http://localhost:8080/voj) ..."
exec "$JAVA_BIN" ${JAVA_OPTS} -jar "$WAR" "$@"
