#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd "$DIR"
VERSION=$(./verify_and_return_version.sh)
echo "Version is $VERSION"
if [[ $VERSION =~ -SNAPSHOT$ ]]; then
    echo "Error: snapshot version"
    exit 1
else
    echo "release version confirmed"
    exit 0
fi