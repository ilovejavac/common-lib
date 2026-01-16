#!/bin/bash
set -e

echo "Spring env: $SPRING_PROFILES_ACTIVE"
echo "JVM OPTS: $JVM_OPTS"

exec java $JVM_OPTS \
  -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
  -Duser.timezone=Asia/Shanghai \
  -Dserver.port=8080 \
  -jar /app/app.jar