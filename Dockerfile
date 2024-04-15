# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
COPY /application/target/ehrbase.jar /app/ehrbase.jar

EXPOSE 8080
ENTRYPOINT  [ "java", "-jar", "-Dspring.profiles.active=docker", "/app/ehrbase.jar"]
