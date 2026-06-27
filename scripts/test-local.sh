#!/usr/bin/env bash
#
# Local test runner for Verwandlung Online Judge.
#
# Runs the web module's test suite against a local MySQL/MariaDB instance
# (defaults to the XAMPP bundle). The judger module's tests require a Linux
# toolchain because they build a JNI native library (see judger/Makefile),
# so they are skipped on macOS — build/test the judger via Docker instead
# (scripts/build-docker.sh) or on a Linux host.
#
# Usage:
#   scripts/test-local.sh
#
# Overridable via environment variables (defaults shown):
#   JDK_HOME=/opt/homebrew/opt/openjdk@25   # Spring 7 supports JDK 17-25
#   MYSQL_BIN=/Applications/XAMPP/bin/mysql
#   DB_HOST=127.0.0.1  DB_PORT=3306  DB_USER=root  DB_PASS=  TEST_DB=test
#   KEEP_DB=0          # set to 1 to keep the test database after the run
#
set -euo pipefail

# Force the build JDK (ignore any stale JAVA_HOME inherited from the shell).
JDK_HOME="${JDK_HOME:-/opt/homebrew/opt/openjdk@25}"
MYSQL_BIN="${MYSQL_BIN:-/Applications/XAMPP/bin/mysql}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
TEST_DB="${TEST_DB:-test}"
KEEP_DB="${KEEP_DB:-0}"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

if [ ! -x "$JDK_HOME/bin/java" ]; then
  echo "ERROR: no JDK at JDK_HOME=$JDK_HOME (install openjdk@25 or override JDK_HOME)." >&2
  exit 1
fi
export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> JDK: $(java -version 2>&1 | head -1)"

mysql_cmd() {
  if [ -n "$DB_PASS" ]; then
    "$MYSQL_BIN" -u "$DB_USER" -p"$DB_PASS" -h "$DB_HOST" -P "$DB_PORT" "$@"
  else
    "$MYSQL_BIN" -u "$DB_USER" -h "$DB_HOST" -P "$DB_PORT" "$@"
  fi
}

# 1. Verify MySQL is reachable.
echo "==> Checking MySQL at ${DB_HOST}:${DB_PORT} ..."
if ! mysql_cmd -e "SELECT VERSION();" >/dev/null 2>&1; then
  echo "ERROR: cannot connect to MySQL at ${DB_HOST}:${DB_PORT}." >&2
  echo "       Start XAMPP (MySQL) first, or override DB_* / MYSQL_BIN." >&2
  exit 1
fi

# 2. The test suite provisions its own schema: the Spring test context runs
#    sql/{schema,seed,test-data}.sql, and the test JDBC URL carries
#    createDatabaseIfNotExist=true plus the required sql_mode. Nothing to load
#    here - the cleanup below just drops the database afterwards (unless KEEP_DB=1).

cleanup() {
  if [ "$KEEP_DB" != "1" ]; then
    echo "==> Dropping test database '${TEST_DB}' ..."
    mysql_cmd -e "DROP DATABASE IF EXISTS \`${TEST_DB}\`;" || true
  fi
}
trap cleanup EXIT

# 3. Run the web module's tests.
echo "==> Running web module tests ..."
mvn test -f web/pom.xml

# 4. Run the judger module's tests on Linux only.
if [ "$(uname -s)" = "Linux" ]; then
  echo "==> Running judger module tests ..."
  cp judger/src/test/resources/voj-test-linux.properties \
     judger/src/test/resources/voj-test.properties
  # The judger build expects jni_md.h / jawt_md.h at $JAVA_HOME/include.
  if [ -f "$JAVA_HOME/include/linux/jni_md.h" ] && [ ! -f "$JAVA_HOME/include/jni_md.h" ]; then
    cp "$JAVA_HOME/include/linux/jni_md.h" "$JAVA_HOME/include/jni_md.h" 2>/dev/null || true
    cp "$JAVA_HOME/include/linux/jawt_md.h" "$JAVA_HOME/include/jawt_md.h" 2>/dev/null || true
  fi
  mvn test -f judger/pom.xml
else
  echo "==> Skipping judger tests on $(uname -s): the judger uses a Linux-only"
  echo "    JNI native build. Use scripts/build-docker.sh or a Linux host."
fi

echo "==> Done."
