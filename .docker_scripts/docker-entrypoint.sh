#!/bin/bash

echo 
echo "EHRBASE_VERSION: $(cat ehrbase_version)"
java -Dspring.profiles.active=docker -jar ehrbase.jar
