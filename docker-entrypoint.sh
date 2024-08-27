#!/bin/sh

java ${JAVA_OPTS} -jar /app/ehrbase.jar --spring.profiles.active=docker ${@}
