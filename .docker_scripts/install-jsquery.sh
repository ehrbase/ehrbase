#!/bin/bash

set -e

# Fetch from remote repository
git clone https://github.com/postgrespro/jsquery.git
cd jsquery

# Build jsQuery plugin
make USE_PGXS=1 && \
make USE_PGXS=1 install && \

cd ..