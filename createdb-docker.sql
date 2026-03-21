-- EHRbase v2 Database Bootstrap
-- Creates: 3 roles, database, 5 schemas, extensions
-- PostgreSQL 18+ required (UUIDv7, WITHOUT OVERLAPS, ltree)

-- ============================================================
-- Environment variables (set via Docker Compose .env)
-- ============================================================
\set db_user `echo "$EHRBASE_USER"`
\set db_pass `echo "$EHRBASE_PASSWORD"`
\set db_user_admin `echo "$EHRBASE_USER_ADMIN"`
\set db_pass_admin `echo "$EHRBASE_PASSWORD_ADMIN"`

-- ============================================================
-- Roles
-- ============================================================
-- ehrbase_admin: Flyway migrations, DDL, schema changes
CREATE ROLE :db_user_admin WITH LOGIN PASSWORD :'db_pass_admin';

-- ehrbase_app: Application (SELECT, INSERT, UPDATE on tables)
CREATE ROLE :db_user WITH LOGIN PASSWORD :'db_pass';

-- ehrbase_audit: Audit event INSERT only (no DELETE, no UPDATE)
CREATE ROLE ehrbase_audit NOLOGIN;

-- ============================================================
-- Database
-- ============================================================
CREATE DATABASE ehrbase
    OWNER :db_user_admin
    ENCODING 'UTF-8'
    LOCALE 'C'
    TEMPLATE template0;

GRANT ALL PRIVILEGES ON DATABASE ehrbase TO :db_user_admin;
GRANT CONNECT ON DATABASE ehrbase TO :db_user;
GRANT CONNECT ON DATABASE ehrbase TO ehrbase_audit;

\c ehrbase

-- ============================================================
-- Schemas (5-schema architecture)
-- ============================================================
-- ehr_system: Core infrastructure (Flyway-managed)
CREATE SCHEMA IF NOT EXISTS ehr_system AUTHORIZATION :db_user_admin;

-- ehr_data: Auto-generated template tables (created by SchemaExecutor)
CREATE SCHEMA IF NOT EXISTS ehr_data AUTHORIZATION :db_user_admin;

-- ehr_views: Auto-generated views (created by SchemaGenerator)
CREATE SCHEMA IF NOT EXISTS ehr_views AUTHORIZATION :db_user_admin;

-- ehr_staging: Migration & import (temporary, Phase 10)
CREATE SCHEMA IF NOT EXISTS ehr_staging AUTHORIZATION :db_user_admin;

-- ext: PostgreSQL extensions
CREATE SCHEMA IF NOT EXISTS ext AUTHORIZATION :db_user_admin;

-- Revoke public schema access
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- ============================================================
-- Default privileges per schema
-- ============================================================

-- ehr_system: app can SELECT, INSERT, UPDATE (no DELETE — pseudonymize instead)
GRANT USAGE ON SCHEMA ehr_system TO :db_user;
ALTER DEFAULT PRIVILEGES FOR ROLE :db_user_admin IN SCHEMA ehr_system
    GRANT SELECT, INSERT, UPDATE ON TABLES TO :db_user;
ALTER DEFAULT PRIVILEGES FOR ROLE :db_user_admin IN SCHEMA ehr_system
    GRANT SELECT ON SEQUENCES TO :db_user;

-- ehr_data: app can full CRUD on template-driven tables
GRANT USAGE ON SCHEMA ehr_data TO :db_user;
GRANT CREATE ON SCHEMA ehr_data TO :db_user;
ALTER DEFAULT PRIVILEGES FOR ROLE :db_user_admin IN SCHEMA ehr_data
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :db_user;

-- ehr_views: app can SELECT only (read-only views)
GRANT USAGE ON SCHEMA ehr_views TO :db_user;
GRANT CREATE ON SCHEMA ehr_views TO :db_user;
ALTER DEFAULT PRIVILEGES FOR ROLE :db_user_admin IN SCHEMA ehr_views
    GRANT SELECT ON TABLES TO :db_user;

-- ehr_staging: app can full CRUD (migration data)
GRANT USAGE ON SCHEMA ehr_staging TO :db_user;
ALTER DEFAULT PRIVILEGES FOR ROLE :db_user_admin IN SCHEMA ehr_staging
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :db_user;

-- ext: app can use extensions
GRANT USAGE ON SCHEMA ext TO :db_user;

-- ehrbase_audit: can only INSERT into ehr_system (audit_event table)
GRANT USAGE ON SCHEMA ehr_system TO ehrbase_audit;

-- ============================================================
-- Extensions (in ext schema)
-- ============================================================
SET search_path TO ext;

CREATE EXTENSION IF NOT EXISTS btree_gist;       -- Required for WITHOUT OVERLAPS temporal constraints
CREATE EXTENSION IF NOT EXISTS pg_trgm;           -- Fuzzy text matching on patient names
CREATE EXTENSION IF NOT EXISTS pgcrypto;          -- Field-level encryption for sensitive data
CREATE EXTENSION IF NOT EXISTS ltree;             -- Hierarchical folder paths
-- NO uuid-ossp — PostgreSQL 18 has uuidv7() built-in

-- ============================================================
-- Database settings
-- ============================================================

-- Search path: ehr_system first, then ehr_data, views, extensions
ALTER ROLE :db_user SET search_path TO ehr_system, ehr_data, ehr_views, ext;
ALTER ROLE :db_user_admin SET search_path TO ehr_system, ehr_data, ehr_views, ext;

-- All timestamps stored in UTC
ALTER DATABASE ehrbase SET timezone TO 'UTC';

-- ISO 8601 interval format
ALTER DATABASE ehrbase SET intervalstyle TO 'iso_8601';
