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
INSERT INTO comp_version (
    vo_id,
    ehr_id,
    contribution_id,
    audit_id,
    template_id,
    sys_version,
    sys_period_lower)
SELECT vo_id,
       ehr_id,
       contribution_id,
       audit_id,
       template_id,
       sys_version,
       sys_period_lower
FROM comp
WHERE num = 0;


INSERT INTO comp_version_history (
    vo_id,
    ehr_id,
    contribution_id,
    audit_id,
    template_id,
    sys_version,
    sys_period_lower,
    sys_period_upper,
    sys_deleted)
SELECT vo_id,
    ehr_id,
    contribution_id,
    audit_id,
    template_id,
    sys_version,
    sys_period_lower,
    sys_period_upper,
    sys_deleted
FROM comp_history
WHERE num = 0;

--EHR_STATUS
INSERT INTO ehr_status_version (
    vo_id,
    ehr_id,
    contribution_id,
    audit_id,
    sys_version,
    sys_period_lower)
SELECT vo_id,
       ehr_id,
       contribution_id,
       audit_id,
       sys_version,
       sys_period_lower
FROM ehr_status
WHERE num = 0;


INSERT INTO ehr_status_version_history (
    vo_id,
    ehr_id,
    contribution_id,
    audit_id,
    sys_version,
    sys_period_lower,
    sys_period_upper,
    sys_deleted)
SELECT vo_id,
       ehr_id,
       contribution_id,
       audit_id,
       sys_version,
       sys_period_lower,
       sys_period_upper,
       sys_deleted
FROM ehr_status_history
WHERE num = 0;

--EHR Folder
INSERT INTO ehr_folder_version (
    vo_id,
    ehr_id,
    contribution_id,
    audit_id,
    sys_version,
    sys_period_lower,
    ehr_folders_idx)
SELECT vo_id,
       ehr_id,
       contribution_id,
       audit_id,
       sys_version,
       sys_period_lower,
       ehr_folders_idx
FROM ehr_folder
WHERE num = 0;


INSERT INTO ehr_folder_version_history (
    vo_id,
    ehr_id,
    contribution_id,
    audit_id,
    sys_version,
    sys_period_lower,
    ehr_folders_idx,
    sys_period_upper,
    sys_deleted)
SELECT vo_id,
       ehr_id,
       contribution_id,
       audit_id,
       sys_version,
       sys_period_lower,
       ehr_folders_idx,
       sys_period_upper,
       sys_deleted
FROM ehr_folder_history
WHERE num = 0;
