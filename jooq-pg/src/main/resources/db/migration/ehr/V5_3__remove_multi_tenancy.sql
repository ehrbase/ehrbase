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


--drop tenant column
ALTER TABLE audit_details DROP COLUMN sys_tenant;
ALTER TABLE comp_history DROP COLUMN sys_tenant;
ALTER TABLE comp DROP COLUMN sys_tenant;
ALTER TABLE contribution DROP COLUMN sys_tenant;
ALTER TABLE ehr_folder_history DROP COLUMN sys_tenant;
ALTER TABLE ehr_folder DROP COLUMN sys_tenant;
ALTER TABLE ehr DROP COLUMN sys_tenant;
ALTER TABLE ehr_status_history DROP COLUMN sys_tenant;
ALTER TABLE ehr_status DROP COLUMN sys_tenant;
ALTER TABLE stored_query DROP COLUMN sys_tenant;
ALTER TABLE template_store DROP COLUMN sys_tenant;
ALTER TABLE users DROP COLUMN sys_tenant;


--recreate foreign keys
ALTER TABLE comp ADD FOREIGN KEY (audit_id) REFERENCES audit_details(id);
ALTER TABLE comp ADD FOREIGN KEY (contribution_id) REFERENCES contribution(id);
ALTER TABLE comp ADD FOREIGN KEY (template_id) REFERENCES template_store(id);
ALTER TABLE comp ADD FOREIGN KEY (ehr_id) REFERENCES ehr(id);
ALTER TABLE comp_history ADD FOREIGN KEY (template_id) REFERENCES template_store(id);
ALTER TABLE comp_history ADD FOREIGN KEY (audit_id) REFERENCES audit_details(id);
ALTER TABLE comp_history ADD FOREIGN KEY (contribution_id) REFERENCES contribution(id);
ALTER TABLE comp_history ADD FOREIGN KEY (ehr_id) REFERENCES ehr(id);
ALTER TABLE contribution ADD FOREIGN KEY (ehr_id) REFERENCES ehr(id) ON DELETE CASCADE;
ALTER TABLE contribution ADD FOREIGN KEY (has_audit) REFERENCES audit_details(id) ON DELETE CASCADE;
ALTER TABLE ehr_folder ADD FOREIGN KEY (audit_id) REFERENCES audit_details(id);
ALTER TABLE ehr_folder ADD FOREIGN KEY (contribution_id) REFERENCES contribution(id);
ALTER TABLE ehr_folder ADD FOREIGN KEY (ehr_id) REFERENCES ehr(id);
ALTER TABLE ehr_folder_history ADD FOREIGN KEY (audit_id) REFERENCES audit_details(id);
ALTER TABLE ehr_folder_history ADD FOREIGN KEY (ehr_id) REFERENCES ehr(id);
ALTER TABLE ehr_folder_history ADD FOREIGN KEY (contribution_id) REFERENCES contribution(id);
ALTER TABLE ehr_status ADD FOREIGN KEY (audit_id) REFERENCES audit_details(id);
ALTER TABLE ehr_status ADD FOREIGN KEY (contribution_id) REFERENCES contribution(id);
ALTER TABLE ehr_status ADD FOREIGN KEY (ehr_id) REFERENCES ehr(id);
ALTER TABLE ehr_status_history ADD FOREIGN KEY (audit_id) REFERENCES audit_details(id);
ALTER TABLE ehr_status_history ADD FOREIGN KEY (contribution_id) REFERENCES contribution(id);
ALTER TABLE ehr_status_history ADD FOREIGN KEY (ehr_id) REFERENCES ehr(id);
