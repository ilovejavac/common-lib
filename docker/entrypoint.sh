#!/usr/bin/env sh
set -eu

exec java ${JVM_OPTS:-} \
  -Dserver.port="${SERVER_PORT:-8080}" \
  -Duser.timezone="${TZ:-Asia/Shanghai}" \
  -jar /app/app.jar "$@"