-- This script needs to be run as database superuser in order to create the database
-- and its required extensions.
-- These operations can not be run by Flyway as they require super user privileged
-- and/or can not be installed inside a transaction.
--
-- Extentions are installed in a separate schema called 'ext'
--
-- For production servers these operations should be performed by a configuration
-- management system.
--
-- If the username, password or database is changed, they also need to be changed
-- in the root build.gradle file.
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

-- create database and roles (you might see an error here, these can be ignored)
CREATE ROLE ehrbase WITH LOGIN PASSWORD 'ehrbase';
CREATE DATABASE ehrbase ENCODING 'UTF-8' TEMPLATE template0;
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase;

-- install the extensions
\c ehrbase
CREATE SCHEMA IF NOT EXISTS ehr AUTHORIZATION ehrbase;
CREATE SCHEMA IF NOT EXISTS ext AUTHORIZATION ehrbase;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA ext;
CREATE EXTENSION IF NOT EXISTS "temporal_tables" SCHEMA ext;
CREATE EXTENSION IF NOT EXISTS "jsquery" SCHEMA ext;
CREATE EXTENSION IF NOT EXISTS "ltree" SCHEMA ext;

-- setup the search_patch so the extensions can be found
ALTER DATABASE ehrbase SET search_path TO "$user",public,ext;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA ext TO ehrbase;
