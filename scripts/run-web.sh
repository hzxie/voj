#!/usr/bin/env bash
#
# Run the Verwandlung Online Judge web application.
#
# Launches the self-contained Spring Boot executable JAR (embedded Tomcat).
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
# sql/{schema,seed,demo}.sql automatically.
#
# Overridable via environment variables (defaults shown):
#   JDK_HOME=/opt/homebrew/opt/openjdk@25   # falls back to `java` on PATH
#   JAVA_OPTS=                              # extra JVM options
#   MYSQL_BIN=mysql                         # mysql client used for checks/import
#   SKIP_PREFLIGHT=                         # set to 1 to skip the service checks
#   JMS_BROKER_EMBEDDED=                    # true/false: host the broker in the web
#                                          # process at runtime, overriding the value
#                                          # baked into the jar (no rebuild needed)
#
set -euo pipefail

JDK_HOME="${JDK_HOME:-/opt/homebrew/opt/openjdk@25}"
# Lean JVM defaults for a small demo: cap the heap and metaspace, use the
# single-threaded GC (smallest native/thread overhead at this heap size) and skip
# the C2 JIT. Export JAVA_OPTS to override the whole set.
JAVA_OPTS="${JAVA_OPTS:--Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:TieredStopAtLevel=1 -Xss512k}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

# Optional runtime override for the broker mode. Passed as a -D system property,
# which Spring ranks above the jar's baked voj.properties, so JMS_BROKER_EMBEDDED
# flips the broker without a rebuild. Empty => use whatever the jar was built with.
JMS_OVERRIDE=""
if [ -n "${JMS_BROKER_EMBEDDED:-}" ]; then
  JMS_OVERRIDE="-Djms.broker.embedded=${JMS_BROKER_EMBEDDED}"
fi

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

JAR="$PROJECT_ROOT/web/target/voj.web.jar"
if [ ! -f "$JAR" ]; then
  echo "ERROR: $JAR not found. Build it first: scripts/build-jars.sh web" >&2
  exit 1
fi

# Warn if the source voj.properties is newer than the packaged jar: config changes
# (e.g. jms.broker.embedded) are baked in at build time, so an edit made after the
# last package run will not take effect until you rebuild — a confusing silent no-op.
SRC_PROPS="$PROJECT_ROOT/web/src/main/resources/voj.properties"
if [ -f "$SRC_PROPS" ] && [ "$SRC_PROPS" -nt "$JAR" ]; then
  echo "WARN: $SRC_PROPS is newer than the packaged jar." >&2
  echo "      Config changes are baked in at build time and will NOT take effect" >&2
  echo "      until you rebuild: scripts/build-jars.sh web (or mvn package -f web/pom.xml)." >&2
fi

# --------------------------------------------------------------------------- #
# Preflight: verify MySQL and ActiveMQ are up, credentials are valid, and the
# voj schema is populated (importing sql/{schema,seed,demo}.sql on first run).
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
  # When the broker is embedded the web process hosts it itself, so there is no
  # external broker to reach yet (it only comes up once the app starts). Skip the
  # check in that mode — requiring the port here would be a chicken-and-egg abort.
  # The runtime override (JMS_BROKER_EMBEDDED) wins over the baked file value, so the
  # check reflects what the app will actually do once launched.
  local jms_embedded
  jms_embedded="${JMS_BROKER_EMBEDDED:-$(prop jms.broker.embedded)}"
  if [ "$jms_embedded" = "true" ]; then
    echo "==> ActiveMQ broker is embedded in the web app; skipping external broker check."
  elif ! tcp_open "$jms_host" "$jms_port"; then
    echo "ERROR: cannot reach ActiveMQ at $jms_host:$jms_port." >&2
    echo "       Start the broker (e.g. 'activemq start') and retry," >&2
    echo "       or set jms.broker.embedded = true to host it in the web app." >&2
    exit 1
  else
    echo "==> Checking ActiveMQ at $jms_host:$jms_port ... ok"
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

  # --- Database: create + import the SQL scripts when the schema is empty - #
  "$MYSQL_BIN" "${mysql_args[@]}" -e \
    "CREATE DATABASE IF NOT EXISTS \`$db_name\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

  local table_count
  table_count="$("$MYSQL_BIN" "${mysql_args[@]}" -N -B -e \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db_name'")"

  if [ "${table_count:-0}" -eq 0 ]; then
    echo "==> Database '$db_name' is empty; importing sql/{schema,seed,demo}.sql ..."
    local sql_name sql_file
    for sql_name in schema.sql seed.sql demo.sql; do
      sql_file="$PROJECT_ROOT/sql/$sql_name"
      if [ ! -f "$sql_file" ]; then
        echo "ERROR: database '$db_name' is empty but $sql_file is missing." >&2
        exit 1
      fi
      echo "    - $sql_name"
      "$MYSQL_BIN" "${mysql_args[@]}" "$db_name" < "$sql_file"
    done
    echo "==> Imported sql/{schema,seed,demo}.sql into '$db_name'."
  else
    echo "==> Database '$db_name' already has $table_count table(s); skipping import."
  fi
}

if [ "${SKIP_PREFLIGHT:-}" != "1" ]; then
  preflight
fi

echo "==> Java: $("$JAVA_BIN" -version 2>&1 | head -1)"
echo "==> Starting voj.web (http://localhost:8080/voj) ..."
exec "$JAVA_BIN" ${JAVA_OPTS} ${JMS_OVERRIDE} -jar "$JAR" "$@"
