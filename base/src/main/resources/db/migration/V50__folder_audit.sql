/*
 *  Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

-- Adds commit_audit to each folder version

-- Migration function to create new dummy audits
CREATE OR REPLACE FUNCTION ehr.migrate_folder_audit(OUT ret_id UUID) AS
$$
BEGIN
    -- Add migration dummy party entry, only if not existing already
    INSERT INTO ehr.party_identified (
        -- id will get generated
        name,
        party_type,
        object_id_type
    )
    SELECT 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0',
           'party_self',
           'undefined'
    WHERE NOT EXISTS (
		SELECT 1 FROM ehr.party_identified WHERE name='migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0'
	);

    -- Helper queries to:
    -- 1) Find the oldest audit to copy two attributes from
    -- (Note: There will always be an audit, because this migration function is only run for existing folder, which require and EHR, which will have a Status, which will have an Audit.
    WITH audits AS (
        SELECT ad.system_id,
               ad.time_committed_tzid
        FROM ehr.audit_details AS ad
        WHERE ad.id IN (
            SELECT id FROM ehr.audit_details ORDER BY time_committed asc LIMIT 1
        )

        ),
    -- 2) Find the dummy party
        party AS (
            SELECT id FROM ehr.party_identified WHERE name  = 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0' LIMIT 1
        )

    -- Copy the values of the oldest/initial audit
    -- and change committer to the dummy party and the description to "migration_dummy"
    INSERT INTO ehr.audit_details (
        -- id will get generated
        system_id,
        committer,
        -- time_committed will get default value
        time_committed_tzid,
        change_type,
        description
    )
    SELECT
        a.system_id,
        p.id,               -- set dummy committer
        a.time_committed_tzid,
        'Unknown',          -- change type set to unknown
        'migration_dummy'   -- description to mark entry as dummy
    FROM audits AS a, party AS p

    -- Finally take and return the ID of the inserted row
    RETURNING id
    INTO  ret_id;  -- returned at the end automatically
END
$$
LANGUAGE plpgsql;

ALTER TABLE ehr.folder
    ADD COLUMN has_audit UUID references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance

ALTER TABLE ehr.folder
    -- Set the type (again), to be able to call the migration function
    ALTER COLUMN has_audit TYPE UUID
    USING ehr.migrate_folder_audit(),
    -- And finally set the column to NOT NULL
    ALTER COLUMN has_audit SET NOT NULL;

ALTER TABLE ehr.folder_history
    ADD COLUMN has_audit UUID references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance

ALTER TABLE ehr.folder_history
    -- Set the type (again), to be able to call the migration function
    ALTER COLUMN has_audit TYPE UUID
    USING ehr.migrate_folder_audit(),
    -- And finally set the column to NOT NULL
    ALTER COLUMN has_audit SET NOT NULL;

-- Also modify the admin deletion of a folder function to include the new audits.
DROP FUNCTION admin_delete_folder(uuid);
-- ====================================================================
-- Description: Function to delete a Folder.
-- Parameters:
--    @folder_id_input - UUID of target Folder
-- Returns: linked contribution, folder children UUIDs, linked audits
-- Requires: Afterwards deletion of all _HISTORY tables with the returned contributions and children + audits.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_folder(folder_id_input UUID)
RETURNS TABLE (contribution UUID, child UUID, audit UUID) AS $$
    DECLARE
        results RECORD;
    BEGIN
        RETURN QUERY WITH
            -- order to delete things:
            -- all folders (scope's parent + children) itself from FOLDER, order shouldn't matter
            -- all their FOLDER_HIERARCHY entries
            -- all FOLDER_ITEMS matching FOLDER.IDs
            -- all OBJECT_REF mentioned in FOLDER_ITEMS
            -- all CONTRIBUTIONs (1..*) collected along the way above
            -- all audits
            -- AFTERWARDS and separate: deletion of all matching *_HISTORY table entries

            -- recursively retrieve all layers of children
            RECURSIVE linked_children AS (
                SELECT child_folder, in_contribution FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
                UNION
                    SELECT fh.child_folder, fh.in_contribution FROM ehr.folder_hierarchy fh
                    INNER JOIN linked_children lc ON lc.child_folder = fh.parent_folder
            ),
            linked_object_ref AS (
                SELECT DISTINCT object_ref_id FROM ehr.folder_items WHERE (folder_id = folder_id_input) OR (folder_id IN (SELECT linked_children.child_folder FROM linked_children))
            ),
            linked_contribution AS (
                SELECT DISTINCT in_contribution FROM ehr.folder WHERE (id = folder_id_input) OR (id IN (SELECT linked_children.child_folder FROM linked_children))

                UNION

                SELECT DISTINCT in_contribution FROM ehr.folder_items WHERE (folder_id = folder_id_input) OR (folder_id IN (SELECT linked_children.child_folder FROM linked_children))

                UNION

                SELECT DISTINCT in_contribution FROM ehr.object_ref WHERE id IN (SELECT linked_object_ref.object_ref_id FROM linked_object_ref)

                UNION

                SELECT DISTINCT in_contribution FROM linked_children
            ),
            linked_audit AS (
                SELECT DISTINCT has_audit FROM ehr.folder WHERE (id = folder_id_input) OR (id IN (SELECT linked_children.child_folder FROM linked_children))
            ),
            remove_directory AS (
                UPDATE ehr.ehr -- remove link to ehr and then actually delete the folder
                SET directory = NULL
                WHERE directory = folder_id_input
            ),
            delete_folders AS (
                DELETE FROM ehr.folder WHERE (id = folder_id_input) OR (id IN (SELECT linked_children.child_folder FROM linked_children))
            ),
            delete_hierarchy AS (
                DELETE FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
            ),
            delete_items AS (
                DELETE FROM ehr.folder_items WHERE (folder_id = folder_id_input) OR (folder_id IN (SELECT linked_children.child_folder FROM linked_children))
            ),
            delete_object_ref AS (
                DELETE FROM ehr.object_ref WHERE id IN (SELECT linked_object_ref.object_ref_id FROM linked_object_ref)
            )
            -- returning contribution IDs to delete separate
            -- same with children IDs, as *_HISTORY tables of ID sets ((original input folder + children), and obj_ref via their contribs) needs to be deleted separate, too.
            -- as well as audits
            SELECT DISTINCT linked_contribution.in_contribution, linked_children.child_folder, linked_audit.has_audit FROM linked_contribution, linked_children, linked_audit;

            -- logging:

            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'FOLDER', folder_id_input, now();
                        -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                            SELECT a.child_folder FROM (
                                SELECT child_folder, in_contribution FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
                            ) AS a )
                        LOOP
                            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'FOLDER', results.child_folder, now();
            END LOOP;

                        RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_HIERARCHY', folder_id_input, now();

                        RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_ITEMS', folder_id_input, now();
                        -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                            SELECT a.child_folder FROM (
                                SELECT child_folder, in_contribution FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
                            ) AS a )
                        LOOP
                            RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_ITEMS', results.child_folder, now();
            END LOOP;

                        -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                            SELECT a.object_ref_id FROM (
                                SELECT DISTINCT object_ref_id FROM ehr.folder_items WHERE (folder_id = folder_id_input) OR (folder_id IN (SELECT b.child_folder FROM (
                                    SELECT child_folder, in_contribution FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
                                ) AS b ))
                            ) AS a
                        )
                        LOOP
                            RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'OBJECT_REF', results.object_ref_id, now();
            END LOOP;
        END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;