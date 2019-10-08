#!/usr/bin/env bash

if [[ "$1" == "" ]]; then
    echo "Error: no version given"
    exit 1
else
    DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
    cd "$DIR/.."
    mvn org.codehaus.mojo:versions-maven-plugin:2.7:set -DnewVersion=$1 -DgenerateBackupPoms=false
fi

