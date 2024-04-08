\set db_user `echo "$EHRBASE_USER"`
\set db_pass `echo "$EHRBASE_PASSWORD"`

\set db_user_admin `echo "$EHRBASE_USER_ADMIN"`
\set db_pass_admin `echo "$EHRBASE_PASSWORD_ADMIN"`

CREATE ROLE :db_user WITH LOGIN PASSWORD :'db_pass';
CREATE ROLE :db_user_admin WITH LOGIN PASSWORD :'db_pass_admin';
CREATE DATABASE ehrbase ENCODING 'UTF-8' LOCALE 'C' TEMPLATE template0;
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO :db_user_admin;
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO :db_user;


\c ehrbase
REVOKE CREATE ON SCHEMA public from PUBLIC;
CREATE SCHEMA IF NOT EXISTS ehr AUTHORIZATION :db_user_admin;
GRANT USAGE ON SCHEMA ehr to :db_user;
ALTER DEFAULT PRIVILEGES FOR USER :db_user_admin IN SCHEMA ehr GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :db_user;
ALTER DEFAULT PRIVILEGES FOR USER :db_user_admin IN SCHEMA ehr GRANT SELECT ON SEQUENCES TO :db_user;

CREATE SCHEMA IF NOT EXISTS ext AUTHORIZATION :db_user_admin;
GRANT USAGE ON SCHEMA ext to :db_user;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA ext;

-- setup the search_patch so the extensions can be found
ALTER DATABASE ehrbase SET search_path TO ext;
-- ensure INTERVAL is ISO8601 encoded
ALTER DATABASE ehrbase SET intervalstyle = 'iso_8601';

ALTER FUNCTION jsonb_path_query(jsonb,jsonpath,jsonb,boolean) ROWS 1;
