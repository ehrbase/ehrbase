FROM adoptopenjdk:11-jre-openj9
ARG JAR_FILE
ENV AUTH_TYPE="BASIC"
ENV AUTH_USER="ehrbase-user"
ENV AUTH_PASSWORD="SuperSecretPassword"
COPY application/target/${JAR_FILE} app.jar
RUN mkdir -p file_repo/knowledge/archetypes &&  mkdir -p file_repo/knowledge/operational_templates && mkdir -p file_repo/knowledge/templates
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=docker", "-Dsecurity.authType=${AUTH_TYPE}", "-Dsecurity.authUser=${AUTH_USER}", "-Dsecurity.authPassword=${AUTH_PASSWORD}" ,"-jar","/app.jar"]
