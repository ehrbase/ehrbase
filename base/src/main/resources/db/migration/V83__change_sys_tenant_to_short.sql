/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- prepare id column




create sequence ehr.sys_tenant_seq;

CREATE OR REPLACE FUNCTION ehr.next_sys_tenant() RETURNS SMALLINT
    LANGUAGE plpgsql
    SECURITY DEFINER SET search_path = ehr, pg_temp AS
$$
BEGIN
    RETURN nextval('ehr.sys_tenant_seq');
END;
$$;
ALTER TABLE ehr.tenant
    DROP
        COLUMN id;

ALTER TABLE ehr.tenant
    ADD COLUMN id smallint default ehr.next_sys_tenant();

-- Make sure the default tenant is 1
DO
$$
    DECLARE
        default_tenant smallint := (select id
                                    from ehr.tenant
                                    where tenant_id = '1f332a66-0e57-11ed-861d-0242ac120002');
    BEGIN
        IF (default_tenant <> 1) THEN
            update ehr.tenant t set id = default_tenant where t.id = 1;
            update ehr.tenant t
            set id = 1
            where t.tenant_id = '1f332a66-0e57-11ed-861d-0242ac120002';
        END IF;
    END
$$;

ALTER TABLE ehr.tenant
    ADD CONSTRAINT tenant_pkey PRIMARY KEY (id);

CREATE TEMP TABLE filtered_namespace_tables AS
SELECT c.table_name as namespace_table_name
FROM information_schema.columns c
         INNER JOIN information_schema.tables t
                    ON c.table_schema = t.table_schema AND c.table_name = t.table_name
WHERE t.table_schema = 'ehr'
  AND c.column_name = 'namespace'
  AND t.table_type = 'BASE TABLE';

-- Add new column sys_tenant to all tables that contain namespace
DO
$$
    DECLARE
        table_name TEXT;
        migration_executing_user TEXT := current_user;
    BEGIN
        FOR table_name IN SELECT namespace_table_name FROM filtered_namespace_tables
            LOOP
                EXECUTE format('CREATE POLICY ehr_policy_ehrbase_migration ON ehr.%I FOR ALL TO %I USING (TRUE)',
                               table_name, migration_executing_user);
                EXECUTE format('ALTER TABLE ehr.%I ADD COLUMN sys_tenant SMALLINT default 1', table_name);
            END LOOP;
    END
$$;

-- updates the sys_tenant column in all tables in the ehr schema with the corresponding id value from the tenant table
-- based on the namespace column, if there is more than one row in the tenant table.
DO
$$
    DECLARE
        table_name TEXT;
    BEGIN
        IF ((SELECT count(id) FROM ehr.tenant) > 1) THEN
            FOR table_name IN SELECT namespace_table_name FROM filtered_namespace_tables
                LOOP
                    EXECUTE format('update ehr.%I t
            set sys_tenant = tnt.id
            from ehr.tenant tnt
            where tnt.tenant_id = t.namespace
              and t.namespace != ''1f332a66-0e57-11ed-861d-0242ac120002''
              and t.sys_tenant = 1', table_name);
                END LOOP;
        END IF;
    END
$$;


-- Create temporary tables before dropping constraints
CREATE TEMP TABLE filtered_tables_foreign_keys AS
    (SELECT conrelid::regclass::text AS table_name, conname, pg_get_constraintdef(oid) as constraintdef
     FROM pg_constraint
     WHERE contype = 'f');

CREATE TEMP TABLE filtered_tables_primary_keys AS
    (SELECT rel.relname, con.conname, pg_get_constraintdef(con.oid)
     FROM pg_catalog.pg_constraint con
              INNER JOIN pg_catalog.pg_class rel
                         ON rel.oid = con.conrelid
              INNER JOIN pg_catalog.pg_namespace nsp
                         ON nsp.oid = connamespace
     WHERE nsp.nspname = 'ehr'
       AND rel.relname IN (SELECT c.table_name
                           FROM information_schema.columns c
                                    INNER JOIN information_schema.tables t
                                               ON c.table_schema = t.table_schema AND c.table_name = t.table_name
                           WHERE t.table_schema = 'ehr'
                             AND c.column_name = 'namespace'
                             AND t.table_type = 'BASE TABLE')
       AND con.contype IN ('p'));

CREATE TEMP TABLE tables_ AS (select *
                              from information_schema.tables);
CREATE TEMP TABLE columns_ AS (select *
                               from information_schema.columns);
CREATE TEMP TABLE table_constraints_ AS (select *
                                         from information_schema.table_constraints);
CREATE TEMP TABLE key_column_usage_ AS (select *
                                        from information_schema.key_column_usage);
CREATE TEMP TABLE constraint_column_usage_ AS (select *
                                               from information_schema.constraint_column_usage);
CREATE TEMP TABLE table_indexdef AS (SELECT pg_get_indexdef(c.oid) AS indexdef,
                                            c.relname              AS index_table
                                     FROM pg_class c
                                              LEFT JOIN pg_attribute a ON a.attrelid = c.oid
                                     WHERE a.attname = 'namespace'
                                       and c.relname not in
                                           ('template_store_pkey', 'entry_composition_id_key', 'status_ehr_id_key',
                                            'users_pkey')
                                       and c.reltype = 0);

-- Dropping all foreign keys
DO
$$
    DECLARE
        FK_records    record;
        foreign_table text;
        query         text;
        s             integer;
    BEGIN
        RAISE NOTICE '----Start dropping constraints-----';
        FOR FK_records IN (select * from filtered_tables_foreign_keys)
            LOOP
                query := '';
                foreign_table := (SELECT ccu.table_name AS foreign_table_name
                                  FROM table_constraints_ AS tc
                                           JOIN key_column_usage_ AS kcu
                                                ON tc.constraint_name = kcu.constraint_name
                                                    AND tc.table_schema = kcu.table_schema
                                           JOIN constraint_column_usage_ AS ccu
                                                ON ccu.constraint_name = tc.constraint_name
                                                    AND ccu.table_schema = tc.table_schema
                                  WHERE tc.constraint_type = 'FOREIGN KEY'
                                    AND tc.constraint_schema = 'ehr'
                                    AND tc.constraint_name = FK_records.conname
                                  limit 1);
                IF length(foreign_table) > 0 THEN
                    s := (SELECT count(*)
                          FROM tables_ t
                                   INNER JOIN columns_ c
                                              ON c.table_schema = t.table_schema AND c.table_name = t.table_name
                          WHERE t.table_schema = 'ehr'
                            AND c.table_name = foreign_table
                            AND c.column_name = 'sys_tenant'
                            AND t.table_type = 'BASE TABLE');

                    IF s > 0 THEN
                        query := 'ALTER TABLE ' || FK_records.table_name || ' DROP CONSTRAINT ' ||
                                 FK_records.conname;
                        RAISE NOTICE '%', query;

                        EXECUTE (query);

                    ELSE
                        query := 'Omitted query: ALTER TABLE ' || FK_records.table_name || ' DROP CONSTRAINT ' ||
                                 FK_records.conname;
                        RAISE NOTICE '%', query;
                    end if;
                end if;
            END LOOP;
        RAISE NOTICE '----Stop dropping constraints-----';
    END
$$;


-- Drop the indexes for the tables that contain namespace
DO
$$
    DECLARE
        indexdef    text;
        index_table text;
        query       TEXT;
    BEGIN
        RAISE NOTICE '----Start dropping indexes where namespace participate----';
        FOR indexdef, index_table IN
            SELECT pg_get_indexdef(c.oid) AS indexdef,
                   c.relname              AS index_table
            FROM pg_class c
                     LEFT JOIN pg_attribute a ON a.attrelid = c.oid
            WHERE a.attname = 'namespace'
              and c.relname not in
                  ('template_store_pkey', 'entry_composition_id_key', 'status_ehr_id_key', 'users_pkey')
              and c.reltype = 0
            LOOP
                IF indexdef IS NOT NULL THEN
                    query := format('DROP INDEX ehr.%I', index_table);
                    RAISE NOTICE '%', query;

                    EXECUTE (query);
                END IF;
            END LOOP;
        RAISE NOTICE '----Stop dropping indexes where namespace participate----';
    END
$$;
ALTER TABLE ehr.status
    DROP CONSTRAINT status_ehr_id_key;
DROP INDEX IF EXISTS status_ehr_idx;
ALTER TABLE ehr.entry
    DROP CONSTRAINT entry_composition_id_key;
DROP INDEX IF EXISTS ehr.entry_composition_id_idx;

-- Creating primary keys
DO
$$
    DECLARE
        table_name      TEXT;
        constraint_name TEXT;
        constraintdef   TEXT;
        query           TEXT;
    BEGIN
        RAISE NOTICE '----Start recreating primary keys----';
        FOR table_name, constraint_name, constraintdef IN (select * from filtered_tables_primary_keys)
            LOOP
                query := 'ALTER TABLE ehr.' || table_name || ' DROP CONSTRAINT ' || constraint_name || ', ADD ' ||
                         REPLACE(REPLACE(REPLACE(constraintdef, '(namespace', '('), ', namespace', ''), ')',
                                 ', sys_tenant)');
                RAISE NOTICE '%', query;

                EXECUTE (query);

            END LOOP;
        RAISE NOTICE '----Stop recreating primary keys----';
    END
$$;

-- Create the indexes for the tables that contain namespace
DO
$$
    DECLARE
        indexdef    TEXT;
        index_table TEXT;
        query       TEXT;
    BEGIN
        RAISE NOTICE '----Start creating indexes where namespace participate----';
        FOR indexdef, index_table IN (select * from table_indexdef)
            LOOP
                query := (
                    REPLACE(REPLACE(indexdef, '(namespace', '(sys_tenant'), ', namespace', ', sys_tenant'));
                RAISE NOTICE '%', query;

                EXECUTE (query);
            END LOOP;
        RAISE NOTICE '----Stop creating indexes where namespace participate----';
    END
$$;

ALTER TABLE ehr.status
    ADD CONSTRAINT status_ehr_id_key UNIQUE (ehr_id, sys_tenant);
ALTER TABLE ehr.entry
    ADD CONSTRAINT entry_composition_id_key UNIQUE (composition_id, sys_tenant);

-- Recreating all foreign keys including sys_tenant
DO
$$
    DECLARE
        FK_records    record;
        foreign_table text;
        query         text;
        s             integer;
    BEGIN
        RAISE NOTICE '----Start recreating foreign keys-----';
        FOR FK_records IN (select * from filtered_tables_foreign_keys)
            LOOP
                query := '';
                foreign_table := (SELECT ccu.table_name AS foreign_table_name
                                  FROM table_constraints_ AS tc
                                           JOIN key_column_usage_ AS kcu
                                                ON tc.constraint_name = kcu.constraint_name
                                                    AND tc.table_schema = kcu.table_schema
                                           JOIN constraint_column_usage_ AS ccu
                                                ON ccu.constraint_name = tc.constraint_name
                                                    AND ccu.table_schema = tc.table_schema
                                  WHERE tc.constraint_type = 'FOREIGN KEY'
                                    AND tc.constraint_schema = 'ehr'
                                    AND tc.constraint_name = FK_records.conname
                                  limit 1);
                IF length(foreign_table) > 0 THEN
                    s := (SELECT count(*)
                          FROM tables_ t
                                   INNER JOIN columns_ c
                                              ON c.table_schema = t.table_schema AND c.table_name = t.table_name
                          WHERE t.table_schema = 'ehr'
                            AND c.table_name = foreign_table
                            AND c.column_name = 'sys_tenant'
                            AND t.table_type = 'BASE TABLE');

                    IF s > 0 THEN
                        query := 'ALTER TABLE ' || FK_records.table_name || ' ADD CONSTRAINT ' ||
                                 FK_records.conname || ' ' ||
                                 REPLACE(FK_records.constraintdef, ')', ', sys_tenant)');
                        RAISE NOTICE '%', query;
                        EXECUTE (query);
                    ELSE
                        query := 'Omitted query: ALTER TABLE ' || FK_records.table_name || ' ADD CONSTRAINT ' ||
                                 FK_records.conname ||
                                 ' ' ||
                                 FK_records.constraintdef;
                        RAISE NOTICE '%', query;
                    end if;
                end if;
            END LOOP;
        RAISE NOTICE '----Stop recreating foreign keys-----';
    END
$$;

-- Create foreign keys for sys_tenant
DO
$$
    DECLARE
        table_name  TEXT;
        column_name TEXT;
        query       TEXT;
    BEGIN
        RAISE NOTICE '----Start creating tenant id foreign keys----';
        FOR table_name, column_name IN
            SELECT c.table_name, c.column_name
            FROM information_schema.columns c
                     INNER JOIN information_schema.tables t
                                ON c.table_schema = t.table_schema AND c.table_name = t.table_name
            WHERE t.table_schema = 'ehr'
              AND c.column_name = 'sys_tenant'
              AND t.table_type = 'BASE TABLE'
            LOOP
                query := format(
                        'ALTER TABLE ehr.%I ADD FOREIGN KEY (sys_tenant) REFERENCES ehr.tenant (id)', table_name);
                RAISE NOTICE '%', query;

                EXECUTE (query);
            END LOOP;
        RAISE NOTICE '----Stop creating tenant id foreign keys----';
    END
$$;

-- Clean up and recreate policies
DO
$$
    DECLARE
        table_name TEXT;
    BEGIN
        FOR table_name IN SELECT namespace_table_name FROM filtered_namespace_tables
            LOOP
                EXECUTE format('DROP POLICY ehr_policy_all ON ehr.%I', table_name);
                EXECUTE format(
                        'CREATE POLICY ehr_policy_all ON ehr.%I FOR ALL USING (sys_tenant = current_setting(''ehrbase.current_tenant'')::smallint)',
                        table_name);
                EXECUTE format('ALTER TABLE ehr.%I DROP COLUMN namespace', table_name);
                EXECUTE format('DROP POLICY ehr_policy_ehrbase_migration ON ehr.%I', table_name);
            END LOOP;
    END
$$;

-- Dropping temporary tables
DROP TABLE IF EXISTS filtered_namespace_tables;
DROP TABLE IF EXISTS filtered_tables_foreign_keys;
DROP TABLE IF EXISTS filtered_tables_primary_keys;
DROP TABLE IF EXISTS table_constraints_;
DROP TABLE IF EXISTS key_column_usage_;
DROP TABLE IF EXISTS constraint_column_usage_;
DROP TABLE IF EXISTS tables_;
DROP TABLE IF EXISTS columns_;
DROP TABLE IF EXISTS table_indexdef;
