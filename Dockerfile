# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S ehrbase && adduser -S ehrbase -G ehrbase

USER ehrbase

WORKDIR /app

COPY /application/target/ehrbase.jar /app/ehrbase.jar
COPY --chown=ehrbase:ehrbase /docker-entrypoint.sh /app/docker-entrypoint

RUN chown -R ehrbase:ehrbase /app &\
    chmod +x /app/docker-entrypoint

EXPOSE 8080

# wrapped in entrypoint to be able to accept cli args and use jacoco cli env var
ENTRYPOINT ["/app/docker-entrypoint"]
