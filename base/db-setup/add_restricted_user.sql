CREATE ROLE ehrbase_restricted WITH LOGIN PASSWORD 'ehrbase_restricted';
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase_restricted;
GRANT USAGE ON SCHEMA ehr to ehrbase_restricted;
alter default privileges for user ehrbase in schema ehr grant select,insert,update,delete on tables to ehrbase_restricted;
GRANT select,insert,update,delete ON ALL TABLES IN SCHEMA ehr TO ehrbase_restricted;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA ext TO ehrbase_restricted;
REVOKE CREATE ON SCHEMA public from PUBLIC;