#!/bin/sh

mvn com.diffplug.spotless:spotless-maven-plugin:apply
git add *.java
