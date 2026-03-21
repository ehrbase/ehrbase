-- JOOQ Code Generation Database Bootstrap
-- Creates schemas and extensions needed for Flyway v2 migrations
-- Runs inside docker-entrypoint-initdb.d on postgres:18

\c ehrbase

-- Schemas
CREATE SCHEMA IF NOT EXISTS ext;
CREATE SCHEMA IF NOT EXISTS ehr_system;
CREATE SCHEMA IF NOT EXISTS ehr_data;
CREATE SCHEMA IF NOT EXISTS ehr_views;
CREATE SCHEMA IF NOT EXISTS ehr_staging;

-- Extensions (in ext schema)
SET search_path TO ext;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS ltree;

-- Roles referenced by compliance migrations
CREATE ROLE ehrbase_restricted NOLOGIN;
CREATE ROLE ehrbase_audit NOLOGIN;
GRANT ehrbase_restricted TO ehrbase;
GRANT ehrbase_audit TO ehrbase;

-- Search path for the user
ALTER ROLE ehrbase SET search_path TO ehr_system, ehr_data, ehr_views, ext;
