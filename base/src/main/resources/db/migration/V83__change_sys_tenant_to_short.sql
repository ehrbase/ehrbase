/*
 *  Copyright (c) 2021 Vitasystems GmbH.
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

-- prepare id column
CREATE SEQUENCE ehr.sys_tenant_id_seq AS SMALLINT;

CREATE OR REPLACE FUNCTION ehr.next() RETURNS SMALLINT
    LANGUAGE plpgsql
    SECURITY DEFINER SET search_path = ehr, pg_temp AS
$$
BEGIN
    RETURN nextval('ehr.sys_tenant_id_seq');
END;
$$;

ALTER TABLE ehr.tenant
    DROP
        COLUMN id;

ALTER TABLE ehr.tenant
    ADD COLUMN id SMALLINT DEFAULT ehr.next();
ALTER TABLE ehr.tenant
    ADD CONSTRAINT tenant_pkey PRIMARY KEY (id);

-- Make sure the default tenant is 1
DO
$$
    DECLARE
        default_tenant smallint := (select id
                                    from ehr.tenant
                                    where tenant_id = '1f332a66-0e57-11ed-861d-0242ac120002');
    BEGIN

        RAISE NOTICE 'tenant %', default_tenant;
        IF (default_tenant <> 1) THEN
            update ehr.tenant t set id = default_tenant where t.id = 1;
            update ehr.tenant t
            set id = 1
            where t.tenant_id = '1f332a66-0e57-11ed-861d-0242ac120002';
        END IF;
    END
$$;

-- Add new column sys_tenant to all tables that contain namespace
DO
$$
    DECLARE
        table_name TEXT;
        column_name
                   TEXT;
    BEGIN
        FOR table_name, column_name IN
            SELECT c.table_name, c.column_name
            FROM information_schema.columns c
                     INNER JOIN information_schema.tables t
                                ON c.table_schema = t.table_schema AND c.table_name = t.table_name
            WHERE t.table_schema = 'ehr'
              AND c.column_name = 'namespace'
              AND t.table_type = 'BASE TABLE'
              AND c.table_name not in('ehr_folder', 'ehr_folder_history')
            LOOP
                EXECUTE format(
                        'CREATE POLICY ehr_policy_ehrbase_migration ON ehr.%I FOR ALL TO ehrbase USING (TRUE)',
                        table_name);
                EXECUTE format('ALTER TABLE ehr.%I ADD COLUMN sys_tenant SMALLINT default 1', table_name);

            END LOOP;
    END
$$;

-- application uses union query that causes the issue if not specify the correct position of the replaceable column
CREATE POLICY ehr_policy_ehrbase_migration ON ehr.ehr_folder FOR ALL TO ehrbase USING (TRUE);
ALTER TABLE ehr.ehr_folder ADD COLUMN sys_tenant SMALLINT default 1;
ALTER TABLE ehr.ehr_folder ADD COLUMN sys_version_new INT;
ALTER TABLE ehr.ehr_folder ADD COLUMN sys_period_lower_new timestamptz;
update ehr.ehr_folder set sys_version_new = efh.sys_version, sys_period_lower_new = efh.sys_period_lower from ehr.ehr_folder efh;
ALTER TABLE ehr.ehr_folder ALTER COLUMN sys_version_new SET NOT NULL;
ALTER TABLE ehr.ehr_folder ALTER COLUMN sys_period_lower_new SET NOT NULL;
ALTER TABLE ehr.ehr_folder DROP COLUMN sys_version;
ALTER TABLE ehr.ehr_folder DROP COLUMN sys_period_lower;
ALTER TABLE ehr.ehr_folder RENAME sys_version_new TO sys_version;
ALTER TABLE ehr.ehr_folder RENAME sys_period_lower_new TO sys_period_lower;

CREATE POLICY ehr_policy_ehrbase_migration ON ehr.ehr_folder_history FOR ALL TO ehrbase USING (TRUE);
ALTER TABLE ehr.ehr_folder_history ADD COLUMN sys_tenant SMALLINT default 1;
ALTER TABLE ehr.ehr_folder_history ADD COLUMN sys_version_new INT;
ALTER TABLE ehr.ehr_folder_history ADD COLUMN sys_period_lower_new timestamptz;
ALTER TABLE ehr.ehr_folder_history ADD COLUMN sys_period_upper_new timestamptz;
ALTER TABLE ehr.ehr_folder_history ADD COLUMN sys_deleted_new boolean;
update ehr.ehr_folder_history set sys_version_new = efh.sys_version, sys_period_lower_new = efh.sys_period_lower, sys_period_upper_new = efh.sys_period_upper, sys_deleted_new = efh.sys_deleted from ehr.ehr_folder_history efh;
ALTER TABLE ehr.ehr_folder_history ALTER COLUMN sys_version_new SET NOT NULL;
ALTER TABLE ehr.ehr_folder_history ALTER COLUMN sys_period_lower_new SET NOT NULL;
ALTER TABLE ehr.ehr_folder_history ALTER COLUMN sys_deleted SET NOT NULL;
ALTER TABLE ehr.ehr_folder_history DROP CONSTRAINT ehr_folder_history_pkey;
ALTER TABLE ehr.ehr_folder_history DROP COLUMN sys_version;
ALTER TABLE ehr.ehr_folder_history DROP COLUMN sys_period_lower;
ALTER TABLE ehr.ehr_folder_history DROP COLUMN sys_period_upper;
ALTER TABLE ehr.ehr_folder_history DROP COLUMN sys_deleted;
ALTER TABLE ehr.ehr_folder_history RENAME sys_version_new TO sys_version;
ALTER TABLE ehr.ehr_folder_history RENAME sys_period_lower_new TO sys_period_lower;
ALTER TABLE ehr.ehr_folder_history RENAME sys_period_upper_new TO sys_period_upper;
ALTER TABLE ehr.ehr_folder_history RENAME sys_deleted_new TO sys_deleted;
ALTER TABLE ehr.ehr_folder_history ADD PRIMARY KEY (ehr_id, id, sys_version);

-- updates the sys_tenant column in all tables in the ehr schema with the corresponding id value from the tenant table
-- based on the namespace column, if there is more than one row in the tenant table.
DO
$$
    DECLARE
        table_name TEXT;
        column_name
                   TEXT;
    BEGIN
        FOR table_name, column_name IN
            SELECT c.table_name, c.column_name
            FROM information_schema.columns c
                     INNER JOIN information_schema.tables t
                                ON c.table_schema = t.table_schema AND c.table_name = t.table_name
            WHERE t.table_schema = 'ehr'
              AND c.column_name = 'namespace'
              AND t.table_type = 'BASE TABLE'
            LOOP
                IF ((SELECT count(id) FROM ehr.tenant) > 1) THEN
                    EXECUTE format('update ehr.%I t
            set sys_tenant = tnt.id
            from ehr.tenant tnt
            where tnt.tenant_id = t.namespace
              and t.namespace != ''1f332a66-0e57-11ed-861d-0242ac120002''
              and t.sys_tenant = 1', table_name);
                END IF;
            END LOOP;
    END
$$;

-- Recreate the indexes for the tables that contain namespace
DO
$$
    DECLARE
        indexdef text;
        index_table
                 text;
    BEGIN
        FOR indexdef, index_table IN
            SELECT pg_get_indexdef(c.oid) AS indexdef,
                   c.relname              AS index_table
            FROM pg_class c
                     LEFT JOIN pg_attribute a ON a.attrelid = c.oid
            WHERE a.attname = 'namespace'
              and c.relname not in
                  ('template_store_pkey', 'entry_composition_id_key', 'status_ehr_id_key', 'users_pkey',
                   'context_participation_index')
              and c.reltype = 0
            LOOP
                IF indexdef IS NOT NULL THEN
                    EXECUTE format('DROP INDEX ehr.%I', index_table);
                    EXECUTE (
                        REPLACE(REPLACE(indexdef, '(namespace', ', sys_tenant'), ', namespace', ', sys_tenant'));
                END IF;
            END LOOP;
    END
$$;

ALTER TABLE ehr.status
    DROP CONSTRAINT status_ehr_id_key;
DROP INDEX IF EXISTS status_ehr_idx;
CREATE UNIQUE INDEX status_ehr_idx ON ehr.status (ehr_id, sys_tenant);
ALTER TABLE ehr.status
    ADD CONSTRAINT status_ehr_id_key UNIQUE USING INDEX status_ehr_idx;

ALTER TABLE ehr.template_store
    DROP CONSTRAINT template_store_pkey,
    ADD PRIMARY KEY (id, template_id, sys_tenant);

ALTER TABLE ehr.users
    DROP CONSTRAINT users_pkey,
    ADD PRIMARY KEY (username, sys_tenant);

DROP INDEX ehr.context_composition_id_idx;
CREATE UNIQUE INDEX context_composition_id_idx ON ehr.event_context (composition_id, sys_tenant);

ALTER TABLE ehr.entry
    DROP CONSTRAINT entry_composition_id_key;
DROP INDEX IF EXISTS ehr.entry_composition_id_idx;
CREATE UNIQUE INDEX entry_composition_id_idx on ehr.entry (composition_id, sys_tenant);
ALTER TABLE ehr.entry
    ADD CONSTRAINT entry_composition_id_key UNIQUE USING INDEX entry_composition_id_idx;

DROP INDEX ehr.context_participation_index;
CREATE INDEX context_participation_index ON ehr.participation (event_context, sys_tenant);

DO
$$
    DECLARE
        table_name  TEXT;
        column_name TEXT;
    BEGIN
        FOR table_name, column_name IN
            SELECT c.table_name, c.column_name
            FROM information_schema.columns c
                     INNER JOIN information_schema.tables t
                                ON c.table_schema = t.table_schema AND c.table_name = t.table_name
            WHERE t.table_schema = 'ehr'
              AND c.column_name = 'sys_tenant'
              AND t.table_type = 'BASE TABLE'
            LOOP
                EXECUTE format(
                        'ALTER TABLE ehr.%I ADD CONSTRAINT %I_fkey FOREIGN KEY (sys_tenant) REFERENCES ehr.tenant (id)',
                        table_name, table_name);
            END LOOP;
    END
$$;


DO
$$
    DECLARE
        table_name  TEXT;
        column_name TEXT;
    BEGIN
        FOR table_name, column_name IN
            SELECT c.table_name, c.column_name
            FROM information_schema.columns c
                     INNER JOIN information_schema.tables t
                                ON c.table_schema = t.table_schema AND c.table_name = t.table_name
            WHERE t.table_schema = 'ehr'
              AND c.column_name = 'sys_tenant'
              AND t.table_type = 'BASE TABLE'
            LOOP
                EXECUTE format('DROP POLICY ehr_policy_all ON ehr.%I', table_name);
                EXECUTE format(
                        'CREATE POLICY ehr_policy_all ON ehr.%I FOR ALL USING (sys_tenant = current_setting(''ehrbase.current_tenant'')::smallint)',
                        table_name);
                EXECUTE format('ALTER TABLE ehr.%I DROP COLUMN namespace', table_name);
                EXECUTE format('DROP POLICY ehr_policy_ehrbase_migration ON ehr.%I',table_name);
            END LOOP;
    END
$$;