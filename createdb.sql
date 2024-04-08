-- This script needs to be run as database superuser in order to create the database
-- These operations can not be run by Flyway as they require super user privileged
-- and/or can not be installed inside a transaction.
--
-- Extentions are installed in a separate schema called 'ext'
--
-- For production servers these operations should be performed by a configuration
-- management system.
--
-- If the username, password or database is changed, they also need to be changed
-- in the root pom.xml file.
--
-- On *NIX run this using:
--
--   sudo -u postgres psql < createdb.sql
--
-- You only have to run this script once.
--
-- THIS WILL NOT CREATE THE ENTIRE DATABASE!
-- It only contains those operations which require superuser privileges.
-- The actual database schema is managed by flyway.
--

CREATE ROLE ehrbase WITH LOGIN PASSWORD 'ehrbase';
CREATE ROLE ehrbase_restricted WITH LOGIN PASSWORD 'ehrbase_restricted';
CREATE DATABASE ehrbase ENCODING 'UTF-8' LOCALE 'C' TEMPLATE template0;
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase;
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase_restricted;


\c ehrbase
REVOKE CREATE ON SCHEMA public from PUBLIC;
CREATE SCHEMA IF NOT EXISTS ehr AUTHORIZATION ehrbase;
GRANT USAGE ON SCHEMA ehr to ehrbase_restricted;
ALTER DEFAULT PRIVILEGES FOR USER ehrbase IN SCHEMA ehr GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ehrbase_restricted;
ALTER DEFAULT PRIVILEGES FOR USER ehrbase IN SCHEMA ehr GRANT SELECT ON SEQUENCES TO ehrbase_restricted;

CREATE SCHEMA IF NOT EXISTS ext AUTHORIZATION ehrbase;
GRANT USAGE ON SCHEMA ext to ehrbase_restricted;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA ext;

-- setup the search_patch so the extensions can be found
ALTER DATABASE ehrbase SET search_path TO ext;
-- ensure INTERVAL is ISO8601 encoded
ALTER DATABASE ehrbase SET intervalstyle = 'iso_8601';

ALTER FUNCTION jsonb_path_query(jsonb,jsonpath,jsonb,boolean) ROWS 1;
