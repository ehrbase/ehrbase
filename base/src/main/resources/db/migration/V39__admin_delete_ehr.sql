-- ====================================================================
-- Author: Jake Smolka
-- Create date: 2020-09-22
-- Description: Admin API functions for physically deletion of objects.
-- =====================================================================


-- ====================================================================
-- Description: Function to delete an audit, incl. system, if not referenced somewhere else.
-- Parameters:
--    @audit_input - UUID of target audit
-- Returns: '1' and linked party UUID
-- Requires: Afterwards deletion of returned party.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_audit(audit_input UUID)
    RETURNS TABLE (num integer, party UUID) AS $$
    BEGIN
        RETURN QUERY WITH
            -- extract info about referenced system, before deleting audit
            scope_system(system_id) AS ( -- get current scope's system ID
                SELECT ehr.audit_details.system_id
                FROM ehr.audit_details
                WHERE id = audit_input
                GROUP BY ehr.audit_details.system_id
            ),
            -- extract info about referenced audits, before deleting audit
            systems_audits(system_id, audit_id) AS ( -- get table of audits and their system ID
                SELECT ehr.system.id AS system_id, ehr.audit_details.id AS audit_id
                FROM ehr.audit_details, ehr.system
                WHERE ehr.system.id = ehr.audit_details.system_id
            ),
            linked_party(id) AS (   -- remember linked party before deletion
                SELECT ehr.audit_details.committer FROM ehr.audit_details WHERE id = audit_input
            ),
            delete_audit_details AS (
                DELETE FROM ehr.audit_details WHERE id = audit_input
            )

            SELECT 1, linked_party.id FROM linked_party;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'AUDIT_DETAILS', audit_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete an attestation.
-- Parameters:
--    @attest_ref_input - UUID of target attestation
-- Returns: linked audit UUID
-- Requires: Afterwards deletion of returned audit.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_attestation(attest_ref_input UUID)
    RETURNS TABLE (audit UUID) AS $$
    DECLARE
        results RECORD;
    BEGIN
        RETURN QUERY WITH
            -- extract info about referenced audit
            linked_audit(id) AS (
                SELECT ehr.attestation.has_audit
                FROM ehr.attestation
                WHERE reference = attest_ref_input
            ),
            -- extract info about attestation linked by the given reference
            linked_attestation(id) AS (
                SELECT ehr.attestation.id
                FROM ehr.attestation
                WHERE reference = attest_ref_input
            ),
            -- extract info about attested_view linked by the extracted attestations
            linked_attested_view(id) AS (
                SELECT ehr.attested_view.id
                FROM ehr.attested_view
                WHERE attestation_id IN (SELECT linked_attestation.id FROM linked_attestation)
            ),
            -- delete attested_view
            delete_attested_view AS (
                DELETE FROM ehr.attested_view WHERE id IN (SELECT linked_attested_view.id FROM linked_attested_view)
            ),
            -- delete attestation
            delete_attestation AS (
                DELETE FROM ehr.attestation WHERE id IN (SELECT linked_attestation.id FROM linked_attestation)
            ),
            -- delete attestation_ref
            delete_attestation_ref AS (
                DELETE FROM ehr.attestation_ref WHERE id = attest_ref_input
            )

            SELECT linked_audit.id FROM linked_audit;

            -- logging:

            -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                SELECT ehr.attested_view.id
                FROM ehr.attested_view
                WHERE attestation_id IN (
                    SELECT a.id FROM (
                        SELECT ehr.attestation.id
                        FROM ehr.attestation
                        WHERE reference = attest_ref_input)
                        AS a
                    )
                )
            LOOP
                RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ATTESTED_VIEW', results.id, now();
            END LOOP;

            -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                SELECT ehr.attestation.id
                FROM ehr.attestation
                WHERE reference = attest_ref_input)
            LOOP
                RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ATTESTATION', results.id, now();
            END LOOP;

            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ATTESTATION_REF', attest_ref_input, now();

    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete event_contexts and participations for a composition and return their parties (event_context.facility and participation.performer).
-- Parameters:
--    @compo_id_input - UUID of super composition
-- Returns: '1' and linked party UUID
-- Requires: Afterwards deletion of returned party.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_event_context_for_compo(compo_id_input UUID)
RETURNS TABLE (num integer, party UUID) AS $$
    DECLARE
        results RECORD;
    BEGIN
        RETURN QUERY WITH 
            linked_events(id) AS ( -- get linked EVENT_CONTEXT entities -- 0..1
                SELECT id, facility FROM ehr.event_context WHERE composition_id = compo_id_input
            ),
            linked_participations_for_events(id) AS ( -- get linked EVENT_CONTEXT entities -- for 0..1 events, each with * participations
                SELECT id, performer FROM ehr.participation WHERE event_context IN (SELECT linked_events.id  FROM linked_events)
            ),
            parties(id) AS (
                SELECT facility FROM linked_events
                UNION
                SELECT performer FROM linked_participations_for_events
            ),
            delete_participation AS (
                DELETE FROM ehr.participation WHERE ehr.participation.id IN (SELECT linked_participations_for_events.id  FROM linked_participations_for_events)
            ),
            delete_event_contexts AS (
                DELETE FROM ehr.event_context WHERE ehr.event_context.id IN (SELECT linked_events.id  FROM linked_events)
            )
            SELECT 1, parties.id FROM parties;

            -- logging:

            -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                SELECT b.id  FROM (
                    SELECT id, performer FROM ehr.participation WHERE event_context IN (SELECT a.id  FROM (
                            SELECT id, facility FROM ehr.event_context WHERE composition_id = compo_id_input
                        ) AS a )
                    ) AS b
            )
            LOOP
                RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'PARTICIPATION', results.id, now();
            END LOOP;

            -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                SELECT id, facility 
                FROM ehr.event_context 
                WHERE composition_id = compo_id_input)
            LOOP
                RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'EVENT_CONTEXT', results.id, now();
            END LOOP;

    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete a single composition, incl. their entries.
-- Parameters:
--    @compo_id_input - UUID of target composition
-- Returns: '1' and linked contribution, party, audit and attestation UUID
-- Requires: Afterwards deletion of returned entities.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_composition(compo_id_input UUID)
RETURNS TABLE (num integer, contribution UUID, party UUID, audit UUID, attestation UUID) AS $$
    DECLARE
        results RECORD;
    BEGIN
        RETURN QUERY WITH linked_entries(id) AS ( -- get linked ENTRY entities
                SELECT id FROM ehr.entry WHERE composition_id = compo_id_input
            ),
            linked_misc(contrib, party, audit, attestation) AS (
                SELECT in_contribution, composer, has_audit, attestation_ref FROM ehr.composition WHERE id = compo_id_input
            ),
            delete_entries AS (
                DELETE FROM ehr.entry WHERE ehr.entry.id IN (SELECT linked_entries.id  FROM linked_entries)
            ),
            -- delete composition itself
            delete_composition AS (
                DELETE FROM ehr.composition WHERE id = compo_id_input
            )
            SELECT 1, linked_misc.contrib, linked_misc.party, linked_misc.audit, linked_misc.attestation FROM linked_misc;

            -- logging:

            -- looping query is reconstructed from above CTEs, because they can't be reused here
            FOR results IN (
                SELECT a.id  
                FROM (
                    SELECT id FROM ehr.entry WHERE composition_id = compo_id_input
                ) AS a )
            LOOP
                RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ENTRY', results.id, now();
            END LOOP;

            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'COMPOSITION', compo_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete a single Composition's history, in entries' history.
-- Necessary as own function, because the former transaction needs to be done to populate the *_history table.
-- Parameters:
--    @compo_input - UUID of target composition
-- Returns: '1'
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_composition_history(compo_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH            
            delete_entry_history AS (
                DELETE FROM ehr.entry_history WHERE composition_id = compo_input
            ),
            delete_composition_history AS (
                DELETE FROM ehr.composition_history WHERE id = compo_input
            )

            SELECT 1;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - Linked to COMPOSITION ID: % - Time: %', 'entry_history', compo_input, now();
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'COMPOSITION_HISTORY', compo_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete a single Contribution.
-- Parameters:
--    @contrib_id_input - UUID of target contribution
-- Returns: '1' and linked audit UUID
-- Requires: Afterwards deletion of returned audit.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_contribution(contrib_id_input UUID)
RETURNS TABLE (num integer, audit UUID) AS $$
    BEGIN
        RETURN QUERY WITH linked_misc(audit) AS (
                SELECT has_audit FROM ehr.contribution WHERE id = contrib_id_input
            ),
            -- delete contribution itself
            delete_composition AS (
                DELETE FROM ehr.contribution WHERE id = contrib_id_input
            )
            SELECT 1, linked_misc.audit FROM linked_misc;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'CONTRIBUTION', contrib_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete an EHR, incl. Status.
-- Parameters:
--    @ehr_id_input - UUID of target EHR
-- Returns: '1' and linked audit, party UUID
-- Requires: Afterwards deletion of returned audit and party.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr(ehr_id_input UUID)
RETURNS TABLE (num integer, status_audit UUID, status_party UUID) AS $$
    BEGIN
        RETURN QUERY WITH linked_status(has_audit) AS ( -- get linked STATUS parameters
                SELECT has_audit FROM ehr.status WHERE ehr_id = ehr_id_input
            ),
            -- delete the EHR itself
            delete_ehr AS (
                DELETE FROM ehr.ehr WHERE id = ehr_id_input
            ),
            linked_party(id) AS (   -- formally always one
                SELECT party FROM ehr.status WHERE ehr_id = ehr_id_input
            ),
            -- Note: not handling the system referenced by EHR, because there is always at least one audit referencing it, too. See separated audit handling.
            -- delete status
            delete_status AS (
                DELETE FROM ehr.status WHERE ehr_id = ehr_id_input
            )

            SELECT 1, linked_status.has_audit, linked_party.id FROM linked_status, linked_party;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'EHR', ehr_id_input, now();
            RAISE NOTICE 'Admin deletion - Type: % - Linked to EHR ID: % - Time: %', 'STATUS', ehr_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete a single EHR's history, meaning the Status' and Contribution's history.
-- Necessary as own function, because the former transaction needs to be done to populate the *_history table.
-- Parameters:
--    @ehr_id_input - UUID of target EHR
-- Returns: '1'
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr_history(ehr_id_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH
            -- delete status_history
            delete_status_history AS (
                DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_input
            ),
            delete_contribution_history AS (
                DELETE FROM ehr.contribution_history WHERE ehr_id = ehr_id_input
            )

            SELECT 1;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - Linked to EHR ID: % - Time: %', 'STATUS_HISTORY', ehr_id_input, now();
            RAISE NOTICE 'Admin deletion - Type: % - Linked to EHR ID: % - Time: %', 'CONTRIBUTION_HISTORY', ehr_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete a Status.
-- Parameters:
--    @status_id_input - UUID of target Status
-- Returns: '1' and linked audit, party UUID
-- Requires: Afterwards deletion of returned audit and party.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_status(status_id_input UUID)
RETURNS TABLE (num integer, status_audit UUID, status_party UUID) AS $$
    BEGIN
        RETURN QUERY WITH
            linked_misc(has_audit, party) AS (   -- formally always one
                SELECT has_audit, party FROM ehr.status WHERE id = status_id_input
            ),
            -- delete status
            delete_status AS (
                DELETE FROM ehr.status WHERE id = status_id_input
            )

            SELECT 1, linked_misc.has_audit, linked_misc.party FROM linked_misc;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'STATUS', status_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete a single Status' history.
-- Necessary as own function, because the former transaction needs to be done to populate the *_history table.
-- Parameters:
--    @status_id_input - UUID of target status
-- Returns: '1'
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_status_history(status_id_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH
            -- delete status_history
            delete_status_history AS (
                DELETE FROM ehr.status_history WHERE id = status_id_input
            )

            SELECT 1;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - Linked to STATUS ID: % - Time: %', 'STATUS_HISTORY', status_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete a Folder.
-- Parameters:
--    @folder_id_input - UUID of target Folder
-- Returns: linked contribution, folder children UUIDs
-- Requires: Afterwards deletion of all _HISTORY tables with the returned contributions and children.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_folder(folder_id_input UUID)
RETURNS TABLE (contribution UUID, child UUID) AS $$
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
            -- AFTERWARDS and separate: deletion of all matching *_HISTORY table entries
            
            linked_children AS (
                SELECT child_folder, in_contribution FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
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
            -- returning contribution IDs to delete separate; same with children IDs, as *_HISTORY tables of ID sets ((original input folder + children), and obj_ref via their contribs) needs to be deleted separate, too.
            SELECT DISTINCT linked_contribution.in_contribution, linked_children.child_folder FROM linked_contribution, linked_children;

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


-- ====================================================================
-- Description: Function to delete some Folder history.
-- Necessary as own function, because the former transaction needs to be done to populate the *_history table.
-- Parameters:
--    @folder_id_input - UUID of target Folder
-- Returns: '1'
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_folder_history(folder_id_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH
            delete_folders AS (
                DELETE FROM ehr.folder_history WHERE id = folder_id_input
            ),
            delete_hierarchy AS (
                DELETE FROM ehr.folder_hierarchy_history WHERE parent_folder = folder_id_input
            ),
            delete_items AS (
                DELETE FROM ehr.folder_items_history WHERE folder_id = folder_id_input
            )

            SELECT 1;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_HISTORY', folder_id_input, now();
            RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_HIERARCHY_HISTORY', folder_id_input, now();
            RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_ITEMS_HISTORY', folder_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to delete the rest of the Folder history.
-- Necessary as own function, because the former transaction needs to be done to populate the *_history table.
-- Parameters:
--    @contribution_id_input - UUID of target contribution, to find the correct object_ref
-- Returns: '1'
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_folder_obj_ref_history(contribution_id_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH
            delete_object_ref AS (
                DELETE FROM ehr.object_ref_history WHERE in_contribution = contribution_id_input
            )

            SELECT 1;

            -- logging:
            RAISE NOTICE 'Admin deletion - Type: % - Linked to CONTRIBUTION ID: % - Time: %', 'OBJECT_REF_HISTORY', contribution_id_input, now();
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to get linked Contributions for an EHR.
-- Parameters:
--    @ehr_id_input - UUID of target EHR
-- Returns: Linked contributions and audits UUIDs
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_get_linked_contributions(ehr_id_input UUID)
RETURNS TABLE (contribution UUID, audit UUID) AS $$
    BEGIN
        RETURN QUERY WITH
            linked_contrib(id, audit) AS ( -- get linked CONTRIBUTION parameters
                SELECT id, has_audit FROM ehr.contribution WHERE ehr_id = ehr_id_input
            )

            SELECT * FROM linked_contrib;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to get linked Compositions for an EHR.
-- Parameters:
--    @ehr_id_input - UUID of target EHR
-- Returns: Linked compositions UUIDs
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_get_linked_compositions(ehr_id_input UUID)
RETURNS TABLE (composition UUID ) AS $$
    BEGIN
        RETURN QUERY WITH
            linked_compo(id) AS ( -- get linked CONTRIBUTION parameters
                SELECT id FROM ehr.composition WHERE ehr_id = ehr_id_input
            )

            SELECT * FROM linked_compo;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to get linked Compositions for a Contribution.
-- Parameters:
--    @contrib_id_input - UUID of target Contribution
-- Returns: Linked compositions UUIDs
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_get_linked_compositions_for_contrib(contrib_id_input UUID)
RETURNS TABLE (composition UUID ) AS $$
    BEGIN
        RETURN QUERY WITH
            linked_compo(id) AS ( -- get linked CONTRIBUTION parameters
                SELECT id FROM ehr.composition WHERE in_contribution = contrib_id_input
            )

            SELECT * FROM linked_compo;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;


-- ====================================================================
-- Description: Function to get linked Status for a Contribution.
-- Parameters:
--    @contrib_id_input - UUID of target Contribution
-- Returns: Linked status UUIDs
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_get_linked_status_for_contrib(contrib_id_input UUID)
RETURNS TABLE (status UUID ) AS $$
    BEGIN
        RETURN QUERY WITH
            linked_status(id) AS ( -- get linked CONTRIBUTION parameters
                SELECT id FROM ehr.status WHERE in_contribution = contrib_id_input
            )

            SELECT * FROM linked_status;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;