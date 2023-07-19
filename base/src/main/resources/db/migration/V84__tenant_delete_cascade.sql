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
-- Create temporary tables before dropping constraints
CREATE TEMP TABLE filtered_tables_foreign_keys AS
    (SELECT conrelid::regclass::text AS table_name, conname, pg_get_constraintdef(oid) as constraintdef
     FROM pg_constraint
     WHERE contype = 'f');

CREATE TEMP TABLE table_constraints_ AS (select *
                                         from information_schema.table_constraints);
CREATE TEMP TABLE key_column_usage_ AS (select *
                                        from information_schema.key_column_usage);
CREATE TEMP TABLE constraint_column_usage_ AS (select *
                                               from information_schema.constraint_column_usage);

CREATE TEMP TABLE filtered_sys_tenant_tables AS
    (SELECT ccu.table_name     AS foreign_table_name,
            ccu.column_name    as foreign_column_name,
            tc.constraint_name,
            kcu.table_name     as table_name,
            ftfk.constraintdef as constraint_def,
            kcu.column_name    as column_name
     FROM table_constraints_ AS tc
              JOIN key_column_usage_ AS kcu
                   ON tc.constraint_name = kcu.constraint_name
                       AND tc.table_schema = kcu.table_schema
              JOIN constraint_column_usage_ AS ccu
                   ON ccu.constraint_name = tc.constraint_name
                       AND ccu.table_schema = tc.table_schema
              JOIN filtered_tables_foreign_keys AS ftfk
                   ON ftfk.conname = tc.constraint_name
     WHERE tc.constraint_type = 'FOREIGN KEY'
       AND tc.constraint_schema = 'ehr'
       AND kcu.column_name = 'sys_tenant'
       AND ccu.table_name = 'tenant');

-- create temporary policy
DO
$$
    DECLARE
        table_name               TEXT;
        migration_executing_user TEXT := current_user;
    BEGIN
        FOR table_name IN SELECT ft.table_name FROM filtered_sys_tenant_tables ft
            LOOP
                EXECUTE format('CREATE POLICY ehr_policy_ehrbase_migration ON ehr.%I FOR ALL TO %I USING (TRUE)',
                               table_name, migration_executing_user);
            END LOOP;
    END
$$;

-- Recreate tenant related foreign keys
DO
$$
    DECLARE
        FK_records record;
        query      text;
    BEGIN
        RAISE NOTICE '----Start recreating foreign keys-----';
        FOR FK_records IN (select * from filtered_sys_tenant_tables)
            LOOP
                query := 'ALTER TABLE  ehr.' || FK_records.table_name || ' DROP CONSTRAINT ' ||
                         FK_records.constraint_name || ', ADD CONSTRAINT ' ||
                         FK_records.constraint_name || ' ' ||
                         REPLACE(FK_records.constraint_def, ' ON DELETE CASCADE', '') ||
                         ' ON DELETE CASCADE';

                RAISE NOTICE '%', query;
                EXECUTE (query);
            END LOOP;
        RAISE NOTICE '----Stop recreating foreign keys-----';
    END
$$;

-- Drop temporary policy
DO
$$
    DECLARE
        table_name TEXT;
    BEGIN
        FOR table_name IN SELECT ft.table_name FROM filtered_sys_tenant_tables ft
            LOOP
                EXECUTE format('DROP POLICY ehr_policy_ehrbase_migration ON ehr.%I', table_name);
            END LOOP;
    END
$$;

-- Dropping temporary tables
DROP TABLE IF EXISTS filtered_tables_foreign_keys;
DROP TABLE IF EXISTS table_constraints_;
DROP TABLE IF EXISTS key_column_usage_;
DROP TABLE IF EXISTS constraint_column_usage_;
DROP TABLE IF EXISTS filtered_sys_tenant_tables;

CREATE OR REPLACE FUNCTION ehr.admin_delete_tenant_full(tenant_id_param smallint)
    RETURNS TABLE
            (
                deleted boolean
            )
    security definer
    SET search_path = ehr, pg_temp
AS
$$
BEGIN
    -- Disable versioning triggers
    ALTER TABLE ehr.composition
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.status
        DISABLE TRIGGER versioning_trigger;

    RETURN QUERY WITH
                     -- Delete data
                     -- ON DELETE CASCADE:
                     --   * ehr.ehr
                     --   * ehr.status
                     --   * ehr.status_history
                     --   * ehr.contribution
                     --   * ehr.attestation
                     --   * ehr.attested_view
                     --   * ehr.composition
                     --   * ehr.composition_history
                     --   * ehr.event_context
                     --   * ehr.event_context_history
                     --   * ehr.participation
                     --   * ehr.participation_history
                     --   * ehr.entry
                     --   * ehr.entry_history
                     --   * ehr.compo_xref
                     --   * ehr.session_log
                     --   * ehr.heading
                     --   * ehr.audit_details
                     --   * ehr.attestation_ref
                     --   * ehr.stored_query
                     --   * ehr.template_store
                     --   * ehr.ehr_folder
                     --   * ehr.ehr_folder_history
                     --   * ehr.users
                     --   * ehr.terminology_provider
                     --   * ehr.party_identified
                     --   * ehr.identifier
                     --   * ehr.access

                     delete_tenant
                         AS (DELETE FROM ehr.tenant WHERE id = tenant_id_param)

                 SELECT true;

-- Restore versioning triggers
    ALTER TABLE ehr.composition
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.status
        ENABLE TRIGGER versioning_trigger;
END
$$
    LANGUAGE plpgsql;
