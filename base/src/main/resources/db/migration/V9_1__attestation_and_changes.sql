/*
 * Copyright (c) 2020 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

-- Based on `v9__audit.sql` this migrations adds attestations and other modifications regarding audits
-- like removal of "versioning" of audits

-- removing "versioning" of audit_details
DROP TRIGGER versioning_trigger ON ehr.audit_details;
DROP TABLE ehr.audit_details_history;
ALTER TABLE ehr.audit_details
    DROP COLUMN sys_period;

-- modify constrains
ALTER TABLE ehr.audit_details
    --ALTER COLUMN system_id SET ON DELETE CASCADE,
    --ALTER COLUMN committer SET ON DELETE CASCADE,
    ALTER COLUMN change_type SET NOT NULL;

-- 1-to-many relation-table to optionally reference attestations from version objects
-- needs to be explicit table, instead of being embedded attribute in attestation table, because can't "references" to different table's IDs
-- necessary because all versioned objects are valid values, but are implemented in their own table (without inheritance)
CREATE TABLE ehr.attestation_ref (
    ref UUID primary key DEFAULT ext.uuid_generate_v4() -- ref key to allow many-relationship
);

-- Also modify attestation (sub-class of audit_details) table
ALTER TABLE ehr.attestation
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- attestation inherits "audit_details", so has one linked instance
    DROP COLUMN contribution_id, -- contribution embedded audit handling was replaced with the above column
    ADD COLUMN reference UUID NOT NULL references ehr.attestation_ref(ref) ON DELETE CASCADE;

-- Finally, modify existing object tables to include new attestations feature
-- add audit capabilities to composition table
ALTER TABLE ehr.composition
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE; -- can have this attestation list (through reference)

ALTER TABLE ehr.composition_history
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE; -- can have this attestation list (through reference)

-- add audit and attestations capabilities to (ehr_)status table
-- (also adding audit columns because they weren't added yet)
ALTER TABLE ehr.status
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE, -- can have this attestation list (through reference)
    ADD COLUMN in_contribution UUID NOT NULL references ehr.contribution(id) ON DELETE CASCADE; -- not directly related to audit, but necessary: reference to contribution

ALTER TABLE ehr.status_history
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE, -- can have this attestation list (through reference)
    ADD COLUMN in_contribution UUID NOT NULL references ehr.contribution(id) ON DELETE CASCADE; -- not directly related to audit, but necessary: reference to contribution

-- TODO include other object types like folders