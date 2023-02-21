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
CREATE DATABASE ehrbase ENCODING 'UTF-8' TEMPLATE template0;
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase;
GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase_restricted;



-- install the extensions
\c ehrbase
REVOKE CREATE ON SCHEMA public from PUBLIC;
CREATE SCHEMA IF NOT EXISTS ehr AUTHORIZATION ehrbase;
GRANT USAGE ON SCHEMA ehr to ehrbase_restricted;
alter default privileges for user ehrbase in schema ehr grant select,insert,update,delete on tables to ehrbase_restricted;
CREATE SCHEMA IF NOT EXISTS ext AUTHORIZATION ehrbase;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA ext;
CREATE EXTENSION IF NOT EXISTS "ltree" SCHEMA ext;

-- setup the search_patch so the extensions can be found
ALTER DATABASE ehrbase SET search_path TO "$user",public,ext;
-- ensure INTERVAL is ISO8601 encoded
alter database ehrbase SET intervalstyle = 'iso_8601';

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA ext TO ehrbase_restricted;

-- load the temporal_tables PLPG/SQL functions to emulate the coded extension
-- original source: https://github.com/nearform/temporal_tables/blob/master/versioning_function.sql
CREATE OR REPLACE FUNCTION ext.versioning()
    RETURNS TRIGGER AS $$
DECLARE
    sys_period text;
    history_table text;
    manipulate jsonb;
    ignore_unchanged_values bool;
    commonColumns text[];
    time_stamp_to_use timestamptz := current_timestamp;
    range_lower timestamptz;
    transaction_info txid_snapshot;
    existing_range tstzrange;
    holder record;
    holder2 record;
    pg_version integer;
BEGIN
    -- version 0.4.0

    IF TG_WHEN != 'BEFORE' OR TG_LEVEL != 'ROW' THEN
        RAISE TRIGGER_PROTOCOL_VIOLATED USING
            MESSAGE = 'function "versioning" must be fired BEFORE ROW';
    END IF;

    IF TG_OP != 'INSERT' AND TG_OP != 'UPDATE' AND TG_OP != 'DELETE' THEN
        RAISE TRIGGER_PROTOCOL_VIOLATED USING
            MESSAGE = 'function "versioning" must be fired for INSERT or UPDATE or DELETE';
    END IF;

    IF TG_NARGS not in (3,4) THEN
        RAISE INVALID_PARAMETER_VALUE USING
            MESSAGE = 'wrong number of parameters for function "versioning"',
            HINT = 'expected 3 or 4 parameters but got ' || TG_NARGS;
    END IF;

    sys_period := TG_ARGV[0];
    history_table := TG_ARGV[1];
    ignore_unchanged_values := TG_ARGV[3];

    IF ignore_unchanged_values AND TG_OP = 'UPDATE' AND NEW IS NOT DISTINCT FROM OLD THEN
        RETURN OLD;
    END IF;

    -- check if sys_period exists on original table
    SELECT atttypid, attndims INTO holder FROM pg_attribute WHERE attrelid = TG_RELID AND attname = sys_period AND NOT attisdropped;
    IF NOT FOUND THEN
        RAISE 'column "%" of relation "%" does not exist', sys_period, TG_TABLE_NAME USING
            ERRCODE = 'undefined_column';
    END IF;
    IF holder.atttypid != to_regtype('tstzrange') THEN
        IF holder.attndims > 0 THEN
            RAISE 'system period column "%" of relation "%" is not a range but an array', sys_period, TG_TABLE_NAME USING
                ERRCODE = 'datatype_mismatch';
        END IF;

        SELECT rngsubtype INTO holder2 FROM pg_range WHERE rngtypid = holder.atttypid;
        IF FOUND THEN
            RAISE 'system period column "%" of relation "%" is not a range of timestamp with timezone but of type %', sys_period, TG_TABLE_NAME, format_type(holder2.rngsubtype, null) USING
                ERRCODE = 'datatype_mismatch';
        END IF;

        RAISE 'system period column "%" of relation "%" is not a range but type %', sys_period, TG_TABLE_NAME, format_type(holder.atttypid, null) USING
            ERRCODE = 'datatype_mismatch';
    END IF;

    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        -- Ignore rows already modified in this transaction
        transaction_info := txid_current_snapshot();
        IF OLD.xmin::text >= (txid_snapshot_xmin(transaction_info) % (2^32)::bigint)::text
            AND OLD.xmin::text <= (txid_snapshot_xmax(transaction_info) % (2^32)::bigint)::text THEN
            IF TG_OP = 'DELETE' THEN
                RETURN OLD;
            END IF;

            RETURN NEW;
        END IF;

        SELECT current_setting('server_version_num')::integer
        INTO pg_version;

        -- to support postgres < 9.6
        IF pg_version < 90600 THEN
            -- check if history table exits
            IF to_regclass(history_table::cstring) IS NULL THEN
                RAISE 'relation "%" does not exist', history_table;
            END IF;
        ELSE
            IF to_regclass(history_table) IS NULL THEN
                RAISE 'relation "%" does not exist', history_table;
            END IF;
        END IF;

        -- check if history table has sys_period
        IF NOT EXISTS(SELECT * FROM pg_attribute WHERE attrelid = history_table::regclass AND attname = sys_period AND NOT attisdropped) THEN
            RAISE 'history relation "%" does not contain system period column "%"', history_table, sys_period USING
                HINT = 'history relation must contain system period column with the same name and data type as the versioned one';
        END IF;

        EXECUTE format('SELECT $1.%I', sys_period) USING OLD INTO existing_range;

        IF existing_range IS NULL THEN
            RAISE 'system period column "%" of relation "%" must not be null', sys_period, TG_TABLE_NAME USING
                ERRCODE = 'null_value_not_allowed';
        END IF;

        IF isempty(existing_range) OR NOT upper_inf(existing_range) THEN
            RAISE 'system period column "%" of relation "%" contains invalid value', sys_period, TG_TABLE_NAME USING
                ERRCODE = 'data_exception',
                DETAIL = 'valid ranges must be non-empty and unbounded on the high side';
        END IF;

        IF TG_ARGV[2] = 'true' THEN
            -- mitigate update conflicts
            range_lower := lower(existing_range);
            IF range_lower >= time_stamp_to_use THEN
                time_stamp_to_use := range_lower + interval '1 microseconds';
            END IF;
        END IF;

        WITH history AS
                 (SELECT attname, atttypid
                  FROM   pg_attribute
                  WHERE  attrelid = history_table::regclass
                    AND    attnum > 0
                    AND    NOT attisdropped),
             main AS
                 (SELECT attname, atttypid
                  FROM   pg_attribute
                  WHERE  attrelid = TG_RELID
                    AND    attnum > 0
                    AND    NOT attisdropped)
        SELECT
            history.attname AS history_name,
            main.attname AS main_name,
            history.atttypid AS history_type,
            main.atttypid AS main_type
        INTO holder
        FROM history
                 INNER JOIN main
                            ON history.attname = main.attname
        WHERE
                history.atttypid != main.atttypid;

        IF FOUND THEN
            RAISE 'column "%" of relation "%" is of type % but column "%" of history relation "%" is of type %',
                holder.main_name, TG_TABLE_NAME, format_type(holder.main_type, null), holder.history_name, history_table, format_type(holder.history_type, null)
                USING ERRCODE = 'datatype_mismatch';
        END IF;

        WITH history AS
                 (SELECT attname
                  FROM   pg_attribute
                  WHERE  attrelid = history_table::regclass
                    AND    attnum > 0
                    AND    NOT attisdropped),
             main AS
                 (SELECT attname
                  FROM   pg_attribute
                  WHERE  attrelid = TG_RELID
                    AND    attnum > 0
                    AND    NOT attisdropped)
        SELECT array_agg(quote_ident(history.attname)) INTO commonColumns
        FROM history
                 INNER JOIN main
                            ON history.attname = main.attname
                                AND history.attname != sys_period;

        EXECUTE ('INSERT INTO ' ||
                 history_table ||
                 '(' ||
                 array_to_string(commonColumns , ',') ||
                 ',' ||
                 quote_ident(sys_period) ||
                 ') VALUES ($1.' ||
                 array_to_string(commonColumns, ',$1.') ||
                 ',tstzrange($2, $3, ''[)''))')
            USING OLD, range_lower, time_stamp_to_use;
    END IF;

    IF TG_OP = 'UPDATE' OR TG_OP = 'INSERT' THEN
        manipulate := jsonb_set('{}'::jsonb, ('{' || sys_period || '}')::text[], to_jsonb(tstzrange(time_stamp_to_use, null, '[)')));

        RETURN jsonb_populate_record(NEW, manipulate);
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

