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

--replace primary keys
ALTER TABLE audit_details DROP CONSTRAINT audit_details_pkey;
ALTER TABLE audit_details ADD PRIMARY KEY (id);

ALTER TABLE comp_history DROP CONSTRAINT comp_history_pkey;
ALTER TABLE comp_history ADD PRIMARY KEY (vo_id, num, sys_version);

ALTER TABLE comp DROP CONSTRAINT comp_pkey;
ALTER TABLE comp ADD PRIMARY KEY (vo_id, num);

ALTER TABLE contribution DROP CONSTRAINT contribution_pkey;
ALTER TABLE contribution ADD PRIMARY KEY (id);

ALTER TABLE ehr_folder_history DROP CONSTRAINT ehr_folder_history_pkey;
ALTER TABLE ehr_folder_history ADD PRIMARY KEY (ehr_id, ehr_folders_idx, num, sys_version);

ALTER TABLE ehr_folder DROP CONSTRAINT ehr_folder_pkey;
ALTER TABLE ehr_folder ADD PRIMARY KEY (ehr_id, ehr_folders_idx, num);

ALTER TABLE ehr DROP CONSTRAINT ehr_pkey;
ALTER TABLE ehr ADD PRIMARY KEY (id);

ALTER TABLE ehr_status_history DROP CONSTRAINT ehr_status_history_pkey;
ALTER TABLE ehr_status_history ADD PRIMARY KEY (ehr_id, num, sys_version);

ALTER TABLE ehr_status DROP CONSTRAINT ehr_status_pkey;
ALTER TABLE ehr_status ADD PRIMARY KEY (ehr_id, num);

ALTER TABLE stored_query DROP CONSTRAINT stored_query_pkey;
ALTER TABLE stored_query ADD PRIMARY KEY (reverse_domain_name, semantic_id, semver);

ALTER TABLE template_store DROP CONSTRAINT template_store_pkey;
ALTER TABLE template_store ADD PRIMARY KEY (id);
