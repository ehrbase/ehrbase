#!/bin/sh

CHANGED_FILES=$(git diff --cached --name-only --diff-filter=ACM -- '*.java')
if [ -n "$CHANGED_FILES" ]; then
    mvn com.diffplug.spotless:spotless-maven-plugin:apply
    git add $CHANGED_FILES;
fi
