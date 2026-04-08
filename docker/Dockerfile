FROM eclipse-temurin:25-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --create-home --home-dir /home/spring spring \
    && mkdir -p /app/log \
    && chown -R 10001:10001 /app /home/spring

WORKDIR /app

COPY --chown=10001:10001 entrypoint.sh /app/entrypoint.sh
ARG JAR_FILE
COPY --chown=10001:10001 ${JAR_FILE} /app/app.jar

RUN chmod +x /app/entrypoint.sh

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

USER 10001

ENTRYPOINT ["/app/entrypoint.sh"]

#-Xms1g
#-Xmx3g
#-XX:MaxMetaspaceSize=256m
#-XX:+HeapDumpOnOutOfMemoryError
#-XX:HeapDumpPath=/app/log/heapdump.hprof
#-XX:-OmitStackTraceInFastThrow
#-Xlog:gc*:file=/app/log/gc_%p_%t.log:time,level,tags:filecount=5,filesize=20M