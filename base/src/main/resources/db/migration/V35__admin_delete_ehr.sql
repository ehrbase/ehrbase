-- physically delete an EHR and all linked entities


-- TODO: check multiple times if function below are separated smart

-- generic function to delete audit, incl. system, if appropriate TODO: and party
CREATE OR REPLACE FUNCTION ehr.admin_delete_audit(audit_input UUID)
    RETURNS TABLE (num integer) AS $$
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
                     -- delete audit linked to status and contribution
                     -- TODO: delete party
                     delete_audit_details AS (
                         DELETE FROM ehr.audit_details WHERE id = audit_input
                     ),
                     count_audits_for_system(system_id, amount) AS (   -- count amount of audits referencing the system ID, which is referenced in this scope
                         SELECT system_id, COUNT(systems_audits.audit_id)
                         FROM systems_audits
                         WHERE systems_audits.system_id IN (SELECT scope_system.system_id FROM scope_system)
                         GROUP BY system_id
                     ),
                     -- delete system, if no other audit references it
                     delete_system AS (
                         DELETE FROM ehr.system WHERE (ehr.system.id IN (SELECT scope_system.system_id FROM scope_system))
                             -- info gathered above needs to indicate only no reference (i.e. empty table result)
                             AND (NOT EXISTS (SELECT count_audits_for_system.amount FROM count_audits_for_system WHERE count_audits_for_system.amount > 1))
                     )

                 SELECT 1;
END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

-- function to delete a single composition and all linked lower tier entities
CREATE OR REPLACE FUNCTION ehr.admin_delete_composition(compo_id_input UUID)
RETURNS TABLE (num integer, contribution UUID, composer UUID, audit UUID, attestation UUID) AS $$
    BEGIN
        RETURN QUERY WITH linked_entries(id) AS ( -- get linked ENTRY entities
                SELECT id, category FROM ehr.entry WHERE composition_id = compo_id_input
            ),
            linked_events(id) AS ( -- get linked EVENT_CONTEXT entities
                SELECT id, facility, setting FROM ehr.event_context WHERE composition_id = compo_id_input
            ),
            linked_misc(contrib, composer, audit, attestation) AS (
                SELECT in_contribution, composer, has_audit, attestation_ref FROM ehr.composition WHERE id = compo_id_input
            ),
            -- TODO-314: delete all linked entities, like audit and contribution, here too
            -- delete composition itself
            delete_composition AS (
                DELETE FROM ehr.composition WHERE id = compo_id_input
            )
            SELECT 1, linked_misc.contrib, linked_misc.composer, linked_misc.audit, linked_misc.attestation FROM linked_misc;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

-- function to delete a single contribution and all linked lower tier entities
CREATE OR REPLACE FUNCTION ehr.admin_delete_contribution(contrib_id_input UUID)
RETURNS TABLE (num integer, audit UUID) AS $$
    BEGIN
        RETURN QUERY WITH linked_misc(audit) AS (
                SELECT has_audit FROM ehr.contribution WHERE id = contrib_id_input
            ),
            -- TODO-314: delete all linked entities here too
            -- delete contribution itself
            delete_composition AS (
                DELETE FROM ehr.contribution WHERE id = contrib_id_input
            )
            SELECT 1, linked_misc.audit FROM linked_misc;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr(ehr_id_input UUID)
RETURNS TABLE (num integer, /*contrib_audit UUID,*/ status_audit UUID/*, composition UUID*/) AS $$
    BEGIN
        RETURN QUERY WITH linked_status(has_audit) AS ( -- get linked STATUS parameters
                SELECT has_audit FROM ehr.status WHERE ehr_id = ehr_id_input
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
            SELECT 1, linked_status.has_audit FROM linked_status;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

-- necessary as own function, because the former transaction needs to be done to populate the *_history table
CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr_history(ehr_id_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH
            -- delete status_history
            -- TODO: delete party
            delete_status_history AS (
                DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_input
            ),
            delete_contribution_history AS (
                DELETE FROM ehr.contribution_history WHERE ehr_id = ehr_id_input
            ),
            delete_composition_history AS (
                DELETE FROM ehr.composition_history WHERE ehr_id = ehr_id_input
            )

            SELECT 1;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

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