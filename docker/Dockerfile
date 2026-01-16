FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache bash tzdata && rm -rf /var/cache/apk/*
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/log && chown -R spring:spring /app

WORKDIR /app

COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

ARG JAR_FILE
COPY ./$JAR_FILE app.jar

ENV TZ=Asia/Shanghai \
    LANG=zh_CN.UTF-8

USER spring

ENTRYPOINT ["./entrypoint.sh"]
