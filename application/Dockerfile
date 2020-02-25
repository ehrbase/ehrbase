FROM openjdk:8-jdk-alpine
ARG JAR_FILE
COPY application/target/${JAR_FILE} app.jar
RUN mkdir -p file_repo/knowledge/archetypes &&  mkdir -p file_repo/knowledge/operational_templates && mkdir -p file_repo/knowledge/templates
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=docker","-jar","/app.jar"]