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

-- removes the contribution_history table and linked triggers etc.

DROP TRIGGER versioning_trigger ON ehr.contribution;

DROP INDEX ehr_contribution_history;

DROP TABLE ehr.contribution_history;

ALTER TABLE ehr.contribution
    DROP COLUMN sys_transaction,
    DROP COLUMN sys_period;

-- following function needs to replaced by modified version without `contribution_history` reference too

-- ====================================================================
-- Description: Function to delete a single EHR's history, meaning the Status' history.
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
                     )

                 SELECT 1;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - Linked to EHR ID: % - Time: %', 'STATUS_HISTORY', ehr_id_input, now();
END;
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;