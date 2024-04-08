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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

--COMPOSITION
ALTER TABLE comp_version
    ADD FOREIGN KEY (ehr_id) REFERENCES ehr (id),
    ADD FOREIGN KEY (contribution_id) REFERENCES contribution (id),
    ADD FOREIGN KEY (audit_id) REFERENCES audit_details (id),
    ADD FOREIGN KEY (template_id) REFERENCES template_store (id);

ALTER TABLE comp RENAME TO comp_data;
ALTER TABLE comp_data
    DROP COLUMN ehr_id,
    DROP COLUMN contribution_id,
    DROP COLUMN audit_id,
    DROP COLUMN template_id,
    DROP COLUMN sys_version,
    DROP COLUMN sys_period_lower;

ALTER TABLE comp_version_history
    ADD FOREIGN KEY (ehr_id) REFERENCES ehr (id),
    ADD FOREIGN KEY (contribution_id) REFERENCES contribution (id),
    ADD FOREIGN KEY (audit_id) REFERENCES audit_details (id),
    ADD FOREIGN KEY (template_id) REFERENCES template_store (id);

alter table comp_history rename to comp_data_history;
ALTER TABLE comp_data_history
    DROP COLUMN ehr_id,
    DROP COLUMN contribution_id,
    DROP COLUMN audit_id,
    DROP COLUMN template_id,
    DROP COLUMN sys_period_lower,
    DROP COLUMN sys_period_upper,
    DROP COLUMN sys_deleted;

ALTER TABLE comp_data
    ADD FOREIGN KEY (vo_id)
        REFERENCES comp_version (vo_id)
        ON DELETE CASCADE;

ALTER TABLE comp_data_history
    ADD FOREIGN KEY (vo_id, sys_version)
        REFERENCES comp_version_history (vo_id, sys_version)
        ON DELETE CASCADE;

--EHR_STATUS
ALTER TABLE ehr_status_version
    ADD FOREIGN KEY (ehr_id) REFERENCES ehr (id),
    ADD FOREIGN KEY (contribution_id) REFERENCES contribution (id),
    ADD FOREIGN KEY (audit_id) REFERENCES audit_details (id);

--FIXME EHR_ID IS PRIMARY KEY; must be added to data tables & set when creating data
ALTER TABLE ehr_status RENAME TO ehr_status_data;
ALTER TABLE ehr_status_data
    DROP COLUMN contribution_id,
    DROP COLUMN audit_id,
    DROP COLUMN sys_version,
    DROP COLUMN sys_period_lower;

ALTER TABLE ehr_status_version_history
    ADD FOREIGN KEY (ehr_id) REFERENCES ehr (id),
    ADD FOREIGN KEY (contribution_id) REFERENCES contribution (id),
    ADD FOREIGN KEY (audit_id) REFERENCES audit_details (id);

ALTER TABLE ehr_status_history rename to ehr_status_data_history;
ALTER TABLE ehr_status_data_history
    DROP COLUMN contribution_id,
    DROP COLUMN audit_id,
    DROP COLUMN sys_period_lower,
    DROP COLUMN sys_period_upper,
    DROP COLUMN sys_deleted;

ALTER TABLE ehr_status_data
    ADD FOREIGN KEY (ehr_id)
        REFERENCES ehr_status_version (ehr_id)
        ON DELETE CASCADE;

ALTER TABLE ehr_status_data_history
    ADD FOREIGN KEY (ehr_id, sys_version)
        REFERENCES ehr_status_version_history (ehr_id, sys_version)
        ON DELETE CASCADE;

--EHR Folder
ALTER TABLE ehr_folder_version
    ADD FOREIGN KEY (ehr_id) REFERENCES ehr (id),
    ADD FOREIGN KEY (contribution_id) REFERENCES contribution (id),
    ADD FOREIGN KEY (audit_id) REFERENCES audit_details (id);

ALTER TABLE ehr_folder RENAME TO ehr_folder_data;
ALTER TABLE ehr_folder_data
    DROP COLUMN contribution_id,
    DROP COLUMN audit_id,
    DROP COLUMN sys_version,
    DROP COLUMN sys_period_lower;

ALTER TABLE ehr_folder_version_history
    ADD FOREIGN KEY (ehr_id) REFERENCES ehr (id),
    ADD FOREIGN KEY (contribution_id) REFERENCES contribution (id),
    ADD FOREIGN KEY (audit_id) REFERENCES audit_details (id);

alter table ehr_folder_history rename to ehr_folder_data_history;
ALTER TABLE ehr_folder_data_history
    DROP COLUMN contribution_id,
    DROP COLUMN audit_id,
    DROP COLUMN sys_period_lower,
    DROP COLUMN sys_period_upper,
    DROP COLUMN sys_deleted;

ALTER TABLE ehr_folder_data
    ADD FOREIGN KEY (ehr_id, ehr_folders_idx)
        REFERENCES ehr_folder_version (ehr_id, ehr_folders_idx)
        ON DELETE CASCADE;

ALTER TABLE ehr_folder_data_history
    ADD FOREIGN KEY (ehr_id, ehr_folders_idx, sys_version)
        REFERENCES ehr_folder_version_history (ehr_id, ehr_folders_idx, sys_version)
        ON DELETE CASCADE;
