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
    system_id UUID references ehr.system(id),
    committer UUID references ehr.party_identified(id),
    time_committed timestamp default NOW(),
    time_committed_tzid TEXT, -- timezone id
    change_type ehr.contribution_change_type,
    description TEXT, -- is a DvCodedText
    sys_period tstzrange NOT NULL -- temporal table column
);

-- Second, setup change history table and trigger
CREATE TABLE ehr.audit_details_history (like ehr.audit_details);
CREATE INDEX ehr_audit_details_history ON ehr.audit_details_history USING BTREE (id);

CREATE TRIGGER versioning_trigger BEFORE INSERT OR UPDATE OR DELETE ON ehr.audit_details
FOR EACH ROW EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.audit_details_history', true);

-- Finally, modify existing object tables to include new audit feature
-- add audit capabilities to contribution table and remove older columns that were part of the early audit implementation
ALTER TABLE ehr.contribution
    ADD COLUMN has_audit UUID references ehr.audit_details(id) ON DELETE CASCADE, -- has this audit_details instance
    DROP COLUMN system_id,
    DROP COLUMN committer,
    DROP COLUMN time_committed,
    DROP COLUMN time_committed_tzid, -- timezone id
    DROP COLUMN change_type,
    DROP COLUMN description;

ALTER TABLE ehr.contribution_history
    ADD COLUMN has_audit UUID references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance

-- add audit capabilities to composition table
ALTER TABLE ehr.composition
    ADD COLUMN has_audit UUID references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance

ALTER TABLE ehr.composition_history
    ADD COLUMN has_audit UUID references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance

-- TODO include other object types like folders