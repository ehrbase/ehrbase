FROM adoptopenjdk/openjdk11:jre-11.0.15_10-alpine
COPY application/target/application-0.22.0-SNAPSHOT.jar ehrbase.jar
EXPOSE 8080
CMD java -Dspring.profiles.active=docker -jar ehrbase.jar