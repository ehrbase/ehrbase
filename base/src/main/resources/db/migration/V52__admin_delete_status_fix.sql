-- ====================================================================
-- Author: Jake Smolka
-- Create date: 2021-07-21
-- Description: Fix for Admin API deletion of old status audits.
-- =====================================================================

-- The following function is copied from its latest state and modified with the fix.

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
                SELECT has_audit FROM ehr.status AS s
                WHERE ehr_id = ehr_id_input
                UNION
                SELECT has_audit FROM ehr.status_history AS sh
                WHERE ehr_id = ehr_id_input
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