#!/bin/bash
set -eu

exec java $JVM_OPTS \
  -Dserver.port=8080 \
  -jar /app/app.jar