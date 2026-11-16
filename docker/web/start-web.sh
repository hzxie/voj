#!/bin/bash
#
# Starts the web application in the voj.web image (run by supervisord, after the
# bundled MariaDB).
#
# - VOJ_JUDGER_API_TOKEN: when it is not set, a random token is generated on the
#   first start, printed once, and kept next to the database (so it survives
#   image updates); run every judger with it. Read it again with
#   `docker exec voj.web cat /var/lib/mariadb/voj-judger-api-token`.
# - VOJ_DEMO=1: on the first start (no user in the database yet), loads the sample
#   problems, contests and accounts of sql/demo.sql, whose passwords equal their
#   usernames (root / root is an administrator). Only for trying VOJ out.
#
# Without the demo data, the application creates an administrator with a random
# password on its first start and prints it once (`docker logs voj.web`).
set -euo pipefail

TOKEN_FILE=/var/lib/mariadb/voj-judger-api-token
if [ -z "${VOJ_JUDGER_API_TOKEN:-}" ]; then
  if [ ! -s "$TOKEN_FILE" ]; then
    head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n' > "$TOKEN_FILE"
    chmod 600 "$TOKEN_FILE"
    echo "Generated VOJ_JUDGER_API_TOKEN=$(cat "$TOKEN_FILE")"
    echo "Run every judger with -e VOJ_JUDGER_API_TOKEN=<that value>."
  fi
  VOJ_JUDGER_API_TOKEN="$(cat "$TOKEN_FILE")"
  export VOJ_JUDGER_API_TOKEN
fi

# A reverse proxy on the Docker host connects through the gateway of the container's
# network (a published port is forwarded from there), so the web application takes the
# client address from the X-Forwarded-For of that address, as well as of loopback (see
# server.tomcat.remoteip.internal-proxies in application.properties). Set
# VOJ_TRUSTED_PROXIES to match the address of a proxy that connects from elsewhere (e.g.
# a container of its own).
if [ -z "${VOJ_TRUSTED_PROXIES:-}" ]; then
  VOJ_TRUSTED_PROXIES='127\.0\.0\.1|0:0:0:0:0:0:0:1|::1'
  # The gateway of the default route, in hexadecimal and little-endian order.
  while read -r _ destination gateway _; do
    if [ "$destination" = "00000000" ] && [ "$gateway" != "00000000" ]; then
      VOJ_TRUSTED_PROXIES+="|$(printf '%d\\.%d\\.%d\\.%d' "0x${gateway:6:2}" \
        "0x${gateway:4:2}" "0x${gateway:2:2}" "0x${gateway:0:2}")"
      break
    fi
  done < /proc/net/route
  export VOJ_TRUSTED_PROXIES
fi

DB_ARGS=(--protocol=TCP -h "${VOJ_DB_HOST:-localhost}" -P "${VOJ_DB_PORT:-3306}"
  -u "${VOJ_DB_USERNAME:-root}" "-p${VOJ_DB_PASSWORD:-}" "${VOJ_DB_NAME:-voj}")
until mysql "${DB_ARGS[@]}" -e 'SELECT 1' >/dev/null 2>&1; do
  echo "Waiting for the database ..."
  sleep 2
done

if [ "${VOJ_DEMO:-0}" = "1" ] \
    && [ "$(mysql "${DB_ARGS[@]}" -N -B -e 'SELECT COUNT(*) FROM voj_users')" = "0" ]; then
  mysql "${DB_ARGS[@]}" < /opt/voj/demo.sql
  echo "Loaded the demo data: every demo account's password is its username (root / root)."
fi

# Lean JVM defaults for the small single-container footprint: capped heap and
# metaspace, the single-threaded GC and no C2 JIT. Keep in sync with scripts/run-web.sh.
exec java -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:TieredStopAtLevel=1 -Xss512k \
  -jar /opt/voj/voj.web.jar
