#!/bin/sh
psql -U postgres -h localhost << END_OF_SCRIPT

DROP DATABASE ehrbase;

\ir ./base/db-setup/createdb.sql

END_OF_SCRIPT
