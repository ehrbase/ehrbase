/*
 * Copyright (c) 2025 vitasystems GmbH.
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

UPDATE comp_version_history d
SET sys_period_lower = pred.sys_period_upper
FROM comp_version_history pred
WHERE d.sys_deleted = true
AND pred.vo_id = d.vo_id
AND pred.sys_version + 1 = d.sys_version
AND d.sys_period_lower != pred.sys_period_upper;

UPDATE ehr_folder_version_history d
SET sys_period_lower = pred.sys_period_upper
FROM ehr_folder_version_history pred
WHERE d.sys_deleted = true
AND pred.ehr_id = d.ehr_id AND pred.ehr_folders_idx = d.ehr_folders_idx
AND pred.sys_version + 1 = d.sys_version
AND d.sys_period_lower != pred.sys_period_upper;
