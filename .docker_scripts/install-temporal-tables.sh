#!/bin/bash

set -e

# Start server
su - postgres -c "pg_ctl -D ${PGDATA} -w start"

# Fetch from branch "mlt" which has full support of Postgres 11
git clone https://github.com/mlt/temporal_tables.git --branch mlt
cd temporal_tables

# Build from source
make PGUSER=postgres &&
make install &&
make installcheck

# Stop server
su - postgres -c "pg_ctl -D ${PGDATA} -w stop"

cd ..