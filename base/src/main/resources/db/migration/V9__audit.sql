/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

-- First, add new audit_details table containing audit data columns
CREATE TABLE ehr.audit_details (
    id UUID primary key DEFAULT ext.uuid_generate_v4(),
    system_id UUID NOT NULL references ehr.system(id) ON DELETE CASCADE,
    committer UUID NOT NULL references ehr.party_identified(id) ON DELETE CASCADE,
    time_committed timestamp default NOW(),
    time_committed_tzid TEXT, -- timezone id
    change_type ehr.contribution_change_type NOT NULL,
    description TEXT -- is a DvCodedText
);

-- 1-to-many relation-table to optionally reference attestations from version objects
-- needs to be explicit table, instead of being embedded attribute in attestation table, because can't "references" to different table's IDs
-- would be necessary since all versioned objects are valid, but all are implemented in their own table (without inheritance)
CREATE TABLE ehr.attestation_ref (
    ref UUID primary key DEFAULT ext.uuid_generate_v4() -- ref key to allow many-relationship
);

-- Also modify attestation (sub-class of audit_details) table
ALTER TABLE ehr.attestation
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- attestation inherits "audit_details", so has one linked instance
    DROP COLUMN contribution_id, -- contribution embedded audit handling was replaced with the above column
    ADD COLUMN reference UUID NOT NULL references ehr.attestation_ref(ref) ON DELETE CASCADE;

-- Finally, modify existing object tables to include new audit feature
-- add audit capabilities to contribution table and remove older columns that were part of the early audit implementation
ALTER TABLE ehr.contribution
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    DROP COLUMN system_id,
    DROP COLUMN committer,
    DROP COLUMN time_committed,
    DROP COLUMN time_committed_tzid, -- timezone id
    DROP COLUMN change_type,
    DROP COLUMN description;

ALTER TABLE ehr.contribution_history
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance

-- add audit capabilities to composition table
ALTER TABLE ehr.composition
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE; -- can have this attestation list (through reference)

ALTER TABLE ehr.composition_history
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE; -- can have this attestation list (through reference)

-- add audit capabilities to (ehr_)status table
ALTER TABLE ehr.status
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE, -- can have this attestation list (through reference)
    ADD COLUMN in_contribution UUID NOT NULL references ehr.contribution(id) ON DELETE CASCADE; -- not directly related to audit, but necessary: reference to contribution

ALTER TABLE ehr.status_history
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    ADD COLUMN attestation_ref UUID references ehr.attestation_ref(ref) ON DELETE CASCADE, -- can have this attestation list (through reference)
    ADD COLUMN in_contribution UUID NOT NULL references ehr.contribution(id) ON DELETE CASCADE; -- not directly related to audit, but necessary: reference to contribution

-- TODO include other object types like folders