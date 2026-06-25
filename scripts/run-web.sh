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
# Before launching it verifies that MySQL and ActiveMQ are running and that the
# database credentials are correct; if the `voj` schema is empty it imports
# voj.sql automatically.
#
# Overridable via environment variables (defaults shown):
#   JDK_HOME=/opt/homebrew/opt/openjdk@25   # falls back to `java` on PATH
#   JAVA_OPTS=                              # extra JVM options
#   MYSQL_BIN=mysql                         # mysql client used for checks/import
#   SKIP_PREFLIGHT=                         # set to 1 to skip the service checks
#
set -euo pipefail

JDK_HOME="${JDK_HOME:-/opt/homebrew/opt/openjdk@25}"
JAVA_OPTS="${JAVA_OPTS:-}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

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

# --------------------------------------------------------------------------- #
# Preflight: verify MySQL and ActiveMQ are up, credentials are valid, and the
# voj schema is populated (importing voj.sql on first run).
# --------------------------------------------------------------------------- #

# Read a "key = value" property from voj.properties (last match wins).
prop() {
  sed -nE "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*(.*[^[:space:]])[[:space:]]*$/\1/p" \
    "$PROPS" | tail -1
}

# Test whether a TCP port is accepting connections. For "localhost" we also try
# 127.0.0.1, since bash /dev/tcp may resolve localhost to IPv6 (::1) while the
# service only listens on IPv4.
tcp_open() {
  local host="$1" port="$2"
  (exec 3<>"/dev/tcp/$host/$port") 2>/dev/null && exec 3>&- && return 0
  if [ "$host" = "localhost" ]; then
    (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null && exec 3>&- && return 0
  fi
  return 1
}

preflight() {
  local PROPS
  PROPS="$PROJECT_ROOT/web/src/main/resources/voj.properties"
  if [ ! -f "$PROPS" ]; then
    echo "WARN: $PROPS not found; skipping preflight checks." >&2
    return 0
  fi

  # --- Parse connection settings from voj.properties --------------------- #
  local jdbc_url db_host db_port db_name db_user db_pass jms_url jms_host jms_port
  jdbc_url="$(prop jdbc.url)"
  db_host="$(printf '%s' "$jdbc_url" | sed -nE 's#^jdbc:mysql://([^:/]+).*#\1#p')"
  db_port="$(printf '%s' "$jdbc_url" | sed -nE 's#^jdbc:mysql://[^:/]+:([0-9]+).*#\1#p')"
  db_name="$(printf '%s' "$jdbc_url" | sed -nE 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#p')"
  db_user="$(prop jdbc.username)"
  db_pass="$(prop jdbc.password)"
  db_host="${db_host:-localhost}"
  db_port="${db_port:-3306}"
  db_name="${db_name:-voj}"
  db_user="${db_user:-root}"

  jms_url="$(prop jms.broker.url)"
  jms_host="$(printf '%s' "$jms_url" | sed -nE 's#^tcp://([^:/]+).*#\1#p')"
  jms_port="$(printf '%s' "$jms_url" | sed -nE 's#^tcp://[^:/]+:([0-9]+).*#\1#p')"
  jms_host="${jms_host:-localhost}"
  jms_port="${jms_port:-61616}"

  # --- ActiveMQ: is the broker port listening? --------------------------- #
  echo "==> Checking ActiveMQ at $jms_host:$jms_port ..."
  if ! tcp_open "$jms_host" "$jms_port"; then
    echo "ERROR: cannot reach ActiveMQ at $jms_host:$jms_port." >&2
    echo "       Start the broker (e.g. 'activemq start') and retry." >&2
    exit 1
  fi

  # --- MySQL: server up + credentials correct ---------------------------- #
  # The web app talks to MySQL over TCP (JDBC), so force the client to use TCP
  # too: a Unix-socket connection could succeed while the TCP port the app needs
  # is closed, masking a real failure.
  echo "==> Checking MySQL at $db_host:$db_port ..."
  if ! command -v "$MYSQL_BIN" >/dev/null 2>&1; then
    echo "WARN: mysql client ('$MYSQL_BIN') not found; skipping MySQL checks" >&2
    echo "      and auto-import. Set MYSQL_BIN to enable them." >&2
    return 0
  fi

  # Build the mysql client argument list (omit -p when password is empty).
  local -a mysql_args=(--protocol=TCP -h "$db_host" -P "$db_port" -u "$db_user")
  [ -n "$db_pass" ] && mysql_args+=("-p$db_pass")

  local err
  if ! err="$("$MYSQL_BIN" "${mysql_args[@]}" -N -e 'SELECT 1' 2>&1 >/dev/null)"; then
    if printf '%s' "$err" | grep -qi 'access denied'; then
      echo "ERROR: MySQL access denied for user '$db_user'@'$db_host'." >&2
      echo "       Fix jdbc.username / jdbc.password in:" >&2
      echo "         $PROPS" >&2
    elif printf '%s' "$err" | grep -qiE "can't connect|connection refused|unknown.*host|2002|2003"; then
      echo "ERROR: cannot reach MySQL at $db_host:$db_port." >&2
      echo "       Start the MySQL server and retry." >&2
    else
      echo "ERROR: failed to connect to MySQL:" >&2
      echo "       $err" >&2
    fi
    exit 1
  fi

  # --- Database: create + import voj.sql when the schema is empty -------- #
  "$MYSQL_BIN" "${mysql_args[@]}" -e \
    "CREATE DATABASE IF NOT EXISTS \`$db_name\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

  local table_count
  table_count="$("$MYSQL_BIN" "${mysql_args[@]}" -N -B -e \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db_name'")"

  if [ "${table_count:-0}" -eq 0 ]; then
    local sql_file="$PROJECT_ROOT/voj.sql"
    if [ ! -f "$sql_file" ]; then
      echo "ERROR: database '$db_name' is empty but $sql_file is missing." >&2
      exit 1
    fi
    echo "==> Database '$db_name' is empty; importing voj.sql ..."
    "$MYSQL_BIN" "${mysql_args[@]}" "$db_name" < "$sql_file"
    echo "==> Imported voj.sql into '$db_name'."
  else
    echo "==> Database '$db_name' already has $table_count table(s); skipping import."
  fi
}

if [ "${SKIP_PREFLIGHT:-}" != "1" ]; then
  preflight
fi

echo "==> Java: $("$JAVA_BIN" -version 2>&1 | head -1)"
echo "==> Starting voj.web (http://localhost:8080/voj) ..."
exec "$JAVA_BIN" ${JAVA_OPTS} -jar "$WAR" "$@"
