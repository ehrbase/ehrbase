/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific LANGUAGE governing permissions and
 * limitations under the License.
 */

-- Drop CONTRIBUTION.state: ContributionState
ALTER TABLE contribution
    DROP COLUMN IF EXISTS state;
DROP TYPE contribution_state;

-- AUDIT_DETAILS.target_type: {CT, CO, ES, EF}
ALTER TABLE audit_details
    ADD COLUMN IF NOT EXISTS target_type varchar NOT NULL DEFAULT 'XX';
-- update from version tables
-- @formatter:off
UPDATE audit_details set target_type='CO' WHERE target_type = 'XX' AND id in (select audit_id from comp_version);
UPDATE audit_details set target_type='CO' WHERE target_type = 'XX' AND id in (select audit_id from comp_version_history);
UPDATE audit_details set target_type='ES' WHERE target_type = 'XX' AND id in (select audit_id from ehr_status_version);
UPDATE audit_details set target_type='ES' WHERE target_type = 'XX' AND id in (select audit_id from ehr_status_version_history);
UPDATE audit_details set target_type='EF' WHERE target_type = 'XX' AND id in (select audit_id from ehr_folder_version);
UPDATE audit_details set target_type='EF' WHERE target_type = 'XX' AND id in (select audit_id from ehr_folder_version_history);
UPDATE audit_details set target_type='CT' WHERE target_type = 'XX';
-- @formatter:on
ALTER TABLE audit_details
    ALTER COLUMN target_type DROP DEFAULT;

-- Index on *_VERSION.sys_period_lower
CREATE INDEX comp_version_sys_period_lower_idx ON comp_version USING btree (sys_period_lower DESC, vo_id ASC);
CREATE INDEX ehr_status_version_sys_period_lower_idx ON ehr_status_version USING btree (sys_period_lower DESC, ehr_id ASC);
--CREATE INDEX ehr_folder_version_sys_period_lower_idx ON ehr.ehr_folder_version USING btree (sys_period_lower ASC, ehr_id ASC);
