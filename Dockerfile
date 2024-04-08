# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
COPY /application/target/ehrbase.jar ehrbase.jar

EXPOSE 8080
ENTRYPOINT  [ "java", "-jar", "-Dspring.profiles.active=docker", "/ehrbase.jar"]
