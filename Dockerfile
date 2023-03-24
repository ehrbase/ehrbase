# syntax=docker/dockerfile:1
FROM eclipse-temurin:17.0.6_10-jdk
COPY /application/target/*.jar ehrbase.jar
COPY .docker_scripts/docker-entrypoint.sh .

EXPOSE 8080
CMD ./docker-entrypoint.sh
