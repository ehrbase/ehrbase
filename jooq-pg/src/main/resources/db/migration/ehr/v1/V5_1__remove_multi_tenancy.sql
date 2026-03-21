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

--drop foreign keys referencing tenant table
ALTER TABLE audit_details DROP CONSTRAINT audit_details_sys_tenant_fkey;
ALTER TABLE comp_history DROP CONSTRAINT comp_history_sys_tenant_fkey;
ALTER TABLE comp DROP CONSTRAINT comp_sys_tenant_fkey;
ALTER TABLE contribution DROP CONSTRAINT contribution_sys_tenant_fkey;
ALTER TABLE ehr_folder_history DROP CONSTRAINT ehr_folder_history_sys_tenant_fkey;
ALTER TABLE ehr_folder DROP CONSTRAINT ehr_folder_sys_tenant_fkey;
ALTER TABLE ehr_status_history DROP CONSTRAINT ehr_status_history_sys_tenant_fkey;
ALTER TABLE ehr_status DROP CONSTRAINT ehr_status_sys_tenant_fkey;
ALTER TABLE ehr DROP CONSTRAINT ehr_sys_tenant_fkey;
ALTER TABLE stored_query DROP CONSTRAINT stored_query_sys_tenant_fkey;
ALTER TABLE template_store DROP CONSTRAINT template_store_sys_tenant_fkey;
ALTER TABLE users DROP CONSTRAINT users_sys_tenant_fkey;

--drop tenant table
DROP TABLE tenant;

--drop foreign keys referencing sys_tenant
ALTER TABLE comp DROP CONSTRAINT comp_audit_id_sys_tenant_fkey;
ALTER TABLE comp DROP CONSTRAINT comp_contribution_id_sys_tenant_fkey;
ALTER TABLE comp DROP CONSTRAINT comp_ehr_id_sys_tenant_fkey;
ALTER TABLE comp DROP CONSTRAINT comp_template_id_sys_tenant_fkey;
ALTER TABLE comp_history DROP CONSTRAINT comp_history_audit_id_sys_tenant_fkey;
ALTER TABLE comp_history DROP CONSTRAINT comp_history_contribution_id_sys_tenant_fkey;
ALTER TABLE comp_history DROP CONSTRAINT comp_history_ehr_id_sys_tenant_fkey;
ALTER TABLE comp_history DROP CONSTRAINT comp_history_template_id_sys_tenant_fkey;
ALTER TABLE contribution DROP CONSTRAINT contribution_ehr_id_fkey;
ALTER TABLE contribution DROP CONSTRAINT contribution_has_audit_fkey;
ALTER TABLE ehr_folder DROP CONSTRAINT ehr_folder_ehr_id_sys_tenant_fkey;
ALTER TABLE ehr_folder DROP CONSTRAINT ehr_folder_audit_id_sys_tenant_fkey;
ALTER TABLE ehr_folder DROP CONSTRAINT ehr_folder_contribution_id_sys_tenant_fkey;
ALTER TABLE ehr_folder_history DROP CONSTRAINT ehr_folder_history_ehr_id_sys_tenant_fkey;
ALTER TABLE ehr_folder_history DROP CONSTRAINT ehr_folder_history_audit_id_sys_tenant_fkey;
ALTER TABLE ehr_folder_history DROP CONSTRAINT ehr_folder_history_contribution_id_sys_tenant_fkey;
ALTER TABLE ehr_status DROP CONSTRAINT ehr_status_ehr_id_sys_tenant_fkey;
ALTER TABLE ehr_status DROP CONSTRAINT ehr_status_audit_id_sys_tenant_fkey;
ALTER TABLE ehr_status DROP CONSTRAINT ehr_status_contribution_id_sys_tenant_fkey;
ALTER TABLE ehr_status_history DROP CONSTRAINT ehr_status_history_ehr_id_sys_tenant_fkey;
ALTER TABLE ehr_status_history DROP CONSTRAINT ehr_status_history_audit_id_sys_tenant_fkey;
ALTER TABLE ehr_status_history DROP CONSTRAINT ehr_status_history_contribution_id_sys_tenant_fkey;

--remove indexes using sys_tenant
DROP INDEX comp_struc_ehr_idx;
DROP INDEX comp_struc_idx;
DROP INDEX contribution_ehr_idx;
DROP INDEX ehr_status_subject;
DROP INDEX template_store_id_unq;
DROP INDEX users_username_idx;


--disable RLS
ALTER TABLE audit_details DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE comp_history DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE comp DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE contribution DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr_folder_history DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr_folder DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr_status_history DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr_status DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE stored_query DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE template_store DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;
ALTER TABLE users DISABLE ROW LEVEL SECURITY, NO FORCE ROW LEVEL SECURITY;


--drop RLS policies
DROP POLICY ehr_policy_all ON audit_details;
DROP POLICY ehr_policy_all ON comp_history;
DROP POLICY ehr_policy_all ON comp;
DROP POLICY ehr_policy_all ON contribution;
DROP POLICY ehr_policy_all ON ehr_folder_history;
DROP POLICY ehr_policy_all ON ehr_folder;
DROP POLICY ehr_policy_all ON ehr;
DROP POLICY ehr_policy_all ON ehr_status_history;
DROP POLICY ehr_policy_all ON ehr_status;
DROP POLICY ehr_policy_all ON stored_query;
DROP POLICY ehr_policy_all ON template_store;
DROP POLICY ehr_policy_all ON users;
