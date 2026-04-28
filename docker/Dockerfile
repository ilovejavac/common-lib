FROM eclipse-temurin:25-jre-noble

RUN groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --create-home --home-dir /home/spring spring \
    && mkdir -p /app/mnt \
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

#        -Xms3g
#        -Xmx3g
#        -XX:+UseG1GC
#        -XX:MaxDirectMemorySize=512m
#        -XX:MaxMetaspaceSize=512m
#        -XX:MaxGCPauseMillis=200
#        -XX:InitiatingHeapOccupancyPercent=55
#        -XX:G1ReservePercent=10
#        -XX:+HeapDumpOnOutOfMemoryError
#        -XX:HeapDumpPath=/app/mnt/log/heapdump.hprof
#        -XX:-OmitStackTraceInFastThrow
#        -Xlog:gc*:file=/app/mnt/log/gc_%p_%t.log:time,level,tags:filecount=5,filesize=20M
#        -Dfile.encoding=UTF-8
#        -Duser.timezone=Asia/Shanghai