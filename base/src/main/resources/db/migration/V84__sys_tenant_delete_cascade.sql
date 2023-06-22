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

CREATE TEMP TABLE filtered_sys_tenant_tables AS
SELECT c.table_name as sys_tenant_table_name
FROM information_schema.columns c
         INNER JOIN information_schema.tables t
                    ON c.table_schema = t.table_schema AND c.table_name = t.table_name
WHERE t.table_schema = 'ehr'
  AND c.column_name = 'sys_tenant'
  AND t.table_type = 'BASE TABLE';

-- create policy
DO
$$
    DECLARE
        table_name               TEXT;
        migration_executing_user TEXT := current_user;
    BEGIN
        FOR table_name IN SELECT sys_tenant_table_name FROM filtered_sys_tenant_tables
            LOOP
                EXECUTE format('CREATE POLICY ehr_policy_ehrbase_migration ON ehr.%I FOR ALL TO %I USING (TRUE)',
                               table_name, migration_executing_user);
            END LOOP;
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
                             AND c.column_name = 'sys_tenant'
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
                                    AND kcu.column_name = 'sys_tenant'
                                    AND tc.constraint_name = FK_records.conname
                                  limit 1);
                IF length(foreign_table) > 0 THEN
                    s := (SELECT count(*)
                          FROM tables_ t
                                   INNER JOIN columns_ c
                                              ON c.table_schema = t.table_schema AND c.table_name = t.table_name
                          WHERE t.table_schema = 'ehr'
                            AND c.table_name = foreign_table
--                             AND c.column_name = 'sys_tenant'
                            AND t.table_type = 'BASE TABLE');

                    IF s > 0 THEN
                        query := 'ALTER TABLE ' || FK_records.table_name || ' DROP CONSTRAINT ' ||
                                 FK_records.conname;
                        RAISE NOTICE '%', query;

                        EXECUTE (query);

                    ELSE
                        query := 'Omitted query: ALTER TABLE ' || FK_records.table_name ||
                                 FK_records.conname || ' DROP CONSTRAINT ';
                        RAISE NOTICE '%', query;
                    end if;
                end if;
            END LOOP;
        RAISE NOTICE '----Stop dropping constraints-----';
    END
$$;

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
                                    AND kcu.column_name = 'sys_tenant'
                                    AND tc.constraint_name = FK_records.conname
                                  limit 1);
                query := 'ALTER TABLE ' || FK_records.table_name || ' ADD CONSTRAINT ' ||
                         FK_records.conname || ' ' || REPLACE(FK_records.constraintdef, ' ON DELETE CASCADE', '') ||
                         ' ON DELETE CASCADE';
                IF length(foreign_table) > 0 THEN
                    s := (SELECT count(*)
                          FROM tables_ t
                                   INNER JOIN columns_ c
                                              ON c.table_schema = t.table_schema AND c.table_name = t.table_name
                          WHERE t.table_schema = 'ehr'
                            AND c.table_name = foreign_table
                            AND t.table_type = 'BASE TABLE');

                    IF s > 0 THEN
                        RAISE NOTICE '%', query;
                        EXECUTE (query);
                    ELSE
                        query := 'Omitted query: ALTER TABLE ' || FK_records.table_name || ' ADD CONSTRAINT ' ||
                                 FK_records.conname ||
                                 ' ' ||
                                 FK_records.constraintdef || ' ON DELETE CASCADE';
                        RAISE NOTICE '%', query;
                    end if;
                end if;
            END LOOP;
        RAISE NOTICE '----Stop recreating foreign keys-----';
    END
$$;


-- Clean up and recreate policies
DO
$$
    DECLARE
        table_name TEXT;
    BEGIN
        FOR table_name IN SELECT sys_tenant_table_name FROM filtered_sys_tenant_tables
            LOOP
                EXECUTE format('DROP POLICY ehr_policy_ehrbase_migration ON ehr.%I', table_name);
            END LOOP;
    END
$$;

-- Dropping temporary tables
DROP TABLE IF EXISTS filtered_sys_tenant_tables;
DROP TABLE IF EXISTS filtered_tables_foreign_keys;
DROP TABLE IF EXISTS filtered_tables_primary_keys;
DROP TABLE IF EXISTS table_constraints_;
DROP TABLE IF EXISTS key_column_usage_;
DROP TABLE IF EXISTS constraint_column_usage_;
DROP TABLE IF EXISTS tables_;
DROP TABLE IF EXISTS columns_;