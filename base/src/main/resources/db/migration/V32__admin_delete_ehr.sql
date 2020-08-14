-- physically delete an EHR and all linked entities


-- TODO: func to delete status

-- TODO: func to delete contribution

-- TODO: func to delete audit_details (incl. check and deletion of system)


/*CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr(UUID)
  RETURNS JSON AS
$$
DECLARE
  ehr_id ALIAS FOR $1;
BEGIN

  IF (ehr_id IS NULL) THEN
    RETURN NULL;
  END IF;

  RETURN (
    SELECT ehr.js_dv_coded_text(description, ehr.js_code_phrase(conceptid :: TEXT, 'openehr'))
    FROM ehr.concept
    WHERE id = ehr_id AND language = 'en'
  );
END
$$
  LANGUAGE plpgsql;*/


CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr(ehr_id_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH linked_status(id) AS ( -- get linked STATUS parameters
                SELECT id/*, has_audit, in_contribution*/ FROM ehr.status WHERE ehr_id = ehr_id_input
            ),
            linked_contrib(id) AS ( -- get linked CONTRIBUTION parameters
                SELECT id FROM ehr.contribution WHERE ehr_id = ehr_id_input
            ),/*
            -- delete status_history, status itself is deleted through cascading
            delete_status AS (
                DELETE FROM ehr.status WHERE ehr_id = ehr_id_input
            ),
            -- delete status_history, status itself is deleted through cascading
            delete_status_history AS (
                DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_input
            ),
            -- delete contribution linked to status; followed by its history table
            delete_contribution AS (
                DELETE FROM ehr.contribution WHERE ehr_id = ehr_id_input
            ),
            delete_contribution_history AS (
                DELETE FROM ehr.contribution_history WHERE ehr_id = ehr_id_input
            ),*/
            -- delete the EHR itself. cascade includes deletion of status.
            delete_ehr AS (
                DELETE FROM ehr.ehr WHERE id = ehr_id_input
            ),
            /*delete_contribution_history AS (    -- TODO: not working right now?! -> works on second execution, why? even if this is executed last in order
                DELETE FROM ehr.contribution_history WHERE ehr_id = ehr_id_input
            ),*/
            -- delete status_history, status itself is deleted through cascading
            delete_status AS (
                DELETE FROM ehr.status WHERE ehr_id = ehr_id_input
            )


            /*,
            -- delete audit linked to status and contribution TODO: delete system and party
            delete_audit_details AS (
                DELETE FROM ehr.audit_details WHERE id IN (SELECT linked_status.has_audit FROM linked_status)
            )*/
            SELECT 1;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr_history(ehr_id_input UUID)
RETURNS TABLE (num integer) AS $$
    BEGIN
        RETURN QUERY WITH linked_status(id) AS ( -- get linked STATUS parameters
                SELECT id/*, has_audit, in_contribution*/ FROM ehr.status WHERE ehr_id = ehr_id_input
            ),
            linked_contrib(id) AS ( -- get linked CONTRIBUTION parameters
                SELECT id FROM ehr.contribution WHERE ehr_id = ehr_id_input
            ),
            -- delete status_history, status itself is deleted through cascading
            delete_status_history AS (
                DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_input
            ),
            delete_contribution_history AS (    -- TODO: not working right now?! -> works on second execution, why? even if this is executed last in order
                DELETE FROM ehr.contribution_history WHERE ehr_id = ehr_id_input
            )

            SELECT 1;
    END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;