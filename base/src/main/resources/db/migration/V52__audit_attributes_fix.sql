/*
 * Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE
    OR REPLACE FUNCTION ehr.migration_audit_system_id(system_id UUID)
    RETURNS UUID AS
$$
BEGIN

    -- Add migration dummy system entry, only if not existing already
    INSERT INTO ehr.system (
        -- id will get generated
        description,
        settings)
    SELECT 'migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e',
           'internal.migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e.org'
    WHERE NOT EXISTS(
            SELECT 1
            FROM ehr.system
            WHERE description = 'migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e'
        );

    IF
        system_id IS NULL THEN
        RETURN (
            SELECT id
            FROM ehr.system
            WHERE description = 'migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e'
            LIMIT 1
        );
    ELSE
        RETURN system_id;
    END IF;

END
$$
    LANGUAGE plpgsql;

CREATE
    OR REPLACE FUNCTION ehr.migration_audit_committer(committer UUID)
    RETURNS UUID AS
$$
BEGIN

    -- Add migration dummy party entry, only if not existing already
    INSERT INTO ehr.party_identified (
        -- id will get generated
        name,
        party_type,
        object_id_type)
    SELECT 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0',
           'party_self',
           'undefined'
    WHERE NOT EXISTS(
            SELECT 1
            FROM ehr.party_identified
            WHERE name = 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0'
        );

    IF
        committer IS NULL THEN
        RETURN (
            SELECT id
            FROM ehr.party_identified
            WHERE name = 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0'
            LIMIT 1
        );
    ELSE
        RETURN committer;
    END IF;

END
$$
    LANGUAGE plpgsql;

CREATE
    OR REPLACE FUNCTION ehr.migration_audit_tzid(time_committed_tzid TEXT)
    RETURNS TEXT AS
$$
BEGIN
    IF
        time_committed_tzid IS NULL THEN
        RETURN (
            'Etc/UTC'
            );
    ELSE
        RETURN time_committed_tzid;
    END IF;
END
$$
    LANGUAGE plpgsql;

-- Fix mandatory attributes with NOT NULL constraint
ALTER TABLE ehr.audit_details
    -- Set the type (again), to be able to call the migration function
    ALTER COLUMN system_id TYPE UUID
        USING ehr.migration_audit_system_id(system_id),
    -- And finally set the column to NOT NULL
    ALTER COLUMN system_id SET NOT NULL,

    -- Set the type (again), to be able to call the migration function
    ALTER
        COLUMN committer TYPE UUID
        USING ehr.migration_audit_committer(committer),
    -- And finally set the column to NOT NULL
    ALTER
        COLUMN committer
        SET NOT NULL,

    -- change_type is set to NOT NULL already

    -- time_committed has valid default now()

    -- Set the type (again), to be able to call the migration function
    ALTER
        COLUMN time_committed_tzid TYPE TEXT
        USING ehr.migration_audit_tzid(time_committed_tzid),
    -- And finally set the column to NOT NULL
    ALTER
        COLUMN time_committed_tzid
        SET NOT NULL;