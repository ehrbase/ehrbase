#!/usr/bin/env bash

# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



# Run with localy installed RF and libraies
robot -e libtest -e future -e circleci -e obsolete -d results --noncritical not-ready -e BDD -L TRACE robot/

# NOTE: below stuff not usable with the recent pre/postcondition implementation
# # Run with Docker | no need to install anything
# docker run --rm -it --env HOST_UID=$(id -u) --env HOST_GID=$(id -g) --network host \
# --volume "$PWD/robot":/home/robot/tests \
# --volume "$PWD/results":/home/robot/results \
# robod0ck/robod0ck --noncritical not-ready -e BDD -L TRACE tests/

# # CLEANUP after test run
# ## 1. Remove all containers and volumes
# docker container rm --force $(docker container ls -q)
# docker wait $(docker container ls -q)
# docker system prune --volumes --force
#
# ## 2. Restart PostgreSQL DB container
# docker run -e POSTGRES_USER=postgres \
#            -e POSTGRES_PASSWORD=postgres \
#            -d -p 5432:5432 ehrbaseorg/ehrbase-database-docker:11.5
