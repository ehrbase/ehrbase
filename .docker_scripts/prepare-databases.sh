#!/bin/bash

set -e

# Start server
su - postgres -c "pg_ctl -D ${PGDATA} -w start"

# Setup schemas and activate extensions
psql --username="$POSTGRES_USER" --dbname "ehrbase" <<-EOSQL
  CREATE SCHEMA IF NOT EXISTS ehr AUTHORIZATION "$EHRBASE_USER";
  CREATE SCHEMA IF NOT EXISTS ext AUTHORIZATION "$EHRBASE_USER";
  CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA ext;
  CREATE EXTENSION IF NOT EXISTS "temporal_tables" SCHEMA ext;
  CREATE EXTENSION IF NOT EXISTS "jsquery" SCHEMA ext;
  CREATE EXTENSION IF NOT EXISTS "ltree" SCHEMA ext;
  ALTER DATABASE ehrbase SET search_path to "$EHRBASE_USER",public,ext;
  GRANT ALL ON ALL FUNCTIONS IN SCHEMA ext TO $EHRBASE_USER;
EOSQL

# Stop server
su - postgres -c "pg_ctl -D ${PGDATA} -w stop"
