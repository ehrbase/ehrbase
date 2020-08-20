-- physically delete an EHR and all linked entities


-- TODO: func to delete status

-- TODO: func to delete contribution

-- TODO: func to delete audit_details (incl. check and deletion of system)


CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr(ehr_id_input UUID)
RETURNS TABLE (num integer, contrib_audit UUID, status_audit UUID) AS $$
    BEGIN
        RETURN QUERY WITH linked_status(id) AS ( -- get linked STATUS parameters
                SELECT id, has_audit/*, in_contribution*/ FROM ehr.status WHERE ehr_id = ehr_id_input
            ),
            linked_contrib(id) AS ( -- get linked CONTRIBUTION parameters
                SELECT id, has_audit FROM ehr.contribution WHERE ehr_id = ehr_id_input
            ),
            -- delete contribution linked to status; followed by its history table
            delete_contribution AS (
                DELETE FROM ehr.contribution WHERE ehr_id = ehr_id_input
            ),
            -- delete the EHR itself
            delete_ehr AS (
                DELETE FROM ehr.ehr WHERE id = ehr_id_input
            ),
            -- Note: not handling the system referenced by EHR, because there is always at least one audit referencing it, too. See audit handling below.
            -- delete status
            delete_status AS (
                DELETE FROM ehr.status WHERE ehr_id = ehr_id_input
            )
            SELECT 1, linked_status.has_audit, linked_contrib.has_audit FROM linked_status, linked_contrib;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr_history(ehr_id_input UUID, status_audit_input UUID, contrib_audit_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH
            -- delete status_history, status itself is deleted through cascading
            -- TODO: delete party
            delete_status_history AS (
                DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_input
            ),
            delete_contribution_history AS (
                DELETE FROM ehr.contribution_history WHERE ehr_id = ehr_id_input
            ),
             -- extract info about referenced system, before deleting audit
            scope_system(system_id) AS ( -- get current scope's system ID
                SELECT ehr.audit_details.system_id
                FROM ehr.audit_details
                WHERE id = status_audit_input OR id = contrib_audit_input
                GROUP BY ehr.audit_details.system_id
            ),
            -- extract info about referenced system, before deleting audit
            systems_audits(system_id, audit_id) AS ( -- get table of audits and their system ID
                SELECT ehr.system.id AS system_id, ehr.audit_details.id AS audit_id
                FROM ehr.audit_details, ehr.system
                WHERE ehr.system.id = ehr.audit_details.system_id
            ),
            -- delete audit linked to status and contribution
            -- TODO: delete party
            delete_audit_details AS (
                DELETE FROM ehr.audit_details WHERE id = status_audit_input OR id = contrib_audit_input
            ),
            count_systems_for_audit(system_id, amount) AS (   -- count amount of audits referencing the system ID, which is referenced in this scope
                SELECT system_id, COUNT(systems_audits.audit_id)
                FROM systems_audits
                WHERE systems_audits.system_id IN (SELECT scope_system.system_id FROM scope_system)
                GROUP BY system_id
            ),
            -- delete system, if no other audit references it
            delete_system AS (
                DELETE FROM ehr.system WHERE (ehr.system.id IN (SELECT scope_system.system_id FROM scope_system))
                    -- info gathered above needs to indicate only no reference (i.e. empty table result)
                    AND (EXISTS (SELECT count_systems_for_audit.amount FROM count_systems_for_audit))
            )

            SELECT 1;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;