/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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

SET search_path = ehr;
(
    SELECT 'Inconsistent EHR_STATUS found'
    FROM ehr_status_version_history root
    LEFT JOIN ehr_status_version_history vh
         ON root.ehr_id = vh.ehr_id
        AND root.vo_id <> vh.vo_id
    LEFT JOIN ehr_status_version v
         ON root.ehr_id = v.ehr_id
        AND root.vo_id <> v.vo_id
    WHERE root.sys_version = 1
       AND (v.vo_id IS NOT NULL OR vh.vo_id IS NOT NULL)
    LIMIT 1
)
UNION
(
    SELECT 'Inconsistent FOLDER found'
    FROM ehr_folder_version_history root
    LEFT JOIN ehr_folder_version_history vh
         ON root.ehr_id = vh.ehr_id
        AND root.ehr_folders_idx = vh.ehr_folders_idx
        AND root.vo_id <> vh.vo_id
    LEFT JOIN ehr_folder_version v
         ON root.ehr_id = v.ehr_id
        AND root.ehr_folders_idx = v.ehr_folders_idx
        AND root.vo_id <> v.vo_id
    WHERE root.sys_version = 1
      AND (v.vo_id IS NOT NULL OR vh.vo_id IS NOT NULL)
    LIMIT 1
);
