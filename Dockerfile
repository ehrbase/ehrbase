# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S ehrbase && adduser -S ehrbase -G ehrbase

COPY /application/target/ehrbase.jar /app/ehrbase.jar

RUN chown -R ehrbase:ehrbase /app
USER ehrbase
WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/ehrbase.jar --spring.profiles.active=docker"]