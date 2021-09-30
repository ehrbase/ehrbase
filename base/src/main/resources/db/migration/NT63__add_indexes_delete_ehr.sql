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

CREATE INDEX CONCURRENTLY IF NOT EXISTS audit_details_system_idx ON ehr.audit_details(system_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS attestation_reference_idx ON ehr.attestation(reference);
CREATE INDEX CONCURRENTLY IF NOT EXISTS attested_view_attestation_idx ON ehr.attested_view(attestation_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS contribution_ehr_idx ON ehr.contribution(ehr_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS entry_history_composition_idx ON ehr.entry_history(composition_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS status_history_ehr_idx ON ehr.status_history(ehr_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS composition_contribution_idx on ehr.composition(in_contribution);


CREATE OR REPLACE FUNCTION ehr.admin_delete_audit(audit_input UUID)
    RETURNS TABLE (num integer, party UUID) AS $$
BEGIN
RETURN QUERY WITH
    linked_party(id) AS (   -- remember linked party before deletion
        SELECT committer FROM ehr.audit_details WHERE id = audit_input
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

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS contribution_audit_idx on ehr.contribution(has_audit);
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS composition_audit_idx on ehr.composition(has_audit);
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS status_audit_idx on ehr.status(has_audit);