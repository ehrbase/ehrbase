#!/bin/sh

echo "Jacoco agent location:   /app/jacoco-agent.jar"
echo "Jacoco destination file: ${JACOCO_RESULT_PATH}"
java -jar -Dspring.profiles.active=docker -javaagent:/app/jacoco-agent.jar=destfile=${JACOCO_RESULT_PATH},append=false,includes=org.ehrbase.* /app/ehrbase.jar ${@}
