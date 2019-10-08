#!/usr/bin/env bash

# Run with localy installed RF and libraies
# robot -d results -L TRACE robot/*.robot

# Run with Docker | no need to install anything
docker run --rm -it --env HOST_UID=$(id -u) --env HOST_GID=$(id -g) --network host \
--volume "$PWD/robot":/home/robot/tests \
--volume "$PWD/results":/home/robot/results \
asyrjasalo/restinstance -e broken -L TRACE --randomize tests tests/bdd_examples/

# CLEANUP after test run
## 1. Remove all containers and volumes
docker container rm --force $(docker container ls -q)
docker wait $(docker container ls -q)
docker system prune --volumes --force

## 2. Restart PostgreSQL DB container
docker run -e POSTGRES_USER=postgres \
           -e POSTGRES_PASSWORD=postgres \
           -d -p 5432:5432 ehrbaseorg/ehrbase-database-docker:11.5
