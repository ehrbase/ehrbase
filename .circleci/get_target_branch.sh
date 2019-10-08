#!/usr/bin/env bash

case $CIRCLE_BRANCH in
    feature*)
        echo "develop"
        exit 0
        ;;
    release* | hotfix*)
        echo "master"
        exit 0
        ;;
    *)
        exit 0
        ;;
esac