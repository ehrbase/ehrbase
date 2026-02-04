/*
 * Copyright (c) 2026 vitasystems GmbH.
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

-- COMPOSITION
ALTER TABLE comp_version_history
    SET (toast_tuple_target = 128),
    ADD COLUMN IF NOT EXISTS ov_ref int DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ov_data text DEFAULT NULL,
    ALTER COLUMN ov_data SET STORAGE MAIN,
    ALTER COLUMN root_concept SET STORAGE PLAIN;

UPDATE comp_version_history vh
SET ov_ref = vh.sys_version,
    ov_data = dh.data_agg
FROM (
         SELECT vo_id, sys_version, string_agg(
            entity_idx || (CASE WHEN num=0 THEN data-'U' ELSE data END)::text,
            E'\n' ORDER BY num ASC
        ) as data_agg
         FROM comp_data_history
         GROUP BY vo_id, sys_version
     ) dh
WHERE (vh.vo_id, vh.sys_version)=(dh.vo_id, dh.sys_version);

DROP TABLE comp_data_history;

--EHR_STATUS
ALTER TABLE ehr_status_version_history
    SET (toast_tuple_target = 128),
    ADD COLUMN IF NOT EXISTS ov_ref int DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ov_data text DEFAULT NULL,
    ALTER COLUMN ov_data SET STORAGE MAIN;

UPDATE ehr_status_version_history vh
SET ov_ref = vh.sys_version,
    ov_data = dh.data_agg
FROM (
         SELECT ehr_id, sys_version, string_agg(
            entity_idx || (CASE WHEN num=0 THEN data-'U' ELSE data END)::text,
            E'\n' ORDER BY num ASC
        ) as data_agg
         FROM ehr_status_data_history
         GROUP BY ehr_id, sys_version
     ) dh
WHERE (vh.ehr_id, vh.sys_version)=(dh.ehr_id, dh.sys_version);

DROP TABLE ehr_status_data_history;

--FOLDER
ALTER TABLE ehr_folder_version_history
    SET (toast_tuple_target = 128),
    ADD COLUMN IF NOT EXISTS ov_ref int DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ov_data text DEFAULT NULL,
    ALTER COLUMN ov_data SET STORAGE MAIN;

UPDATE ehr_folder_version_history vh
SET ov_ref = vh.sys_version,
    ov_data = dh.data_agg
FROM (
         SELECT
            ehr_id, ehr_folders_idx, sys_version,
            string_agg(
                entity_idx || jsonb_set(
                    CASE WHEN num=0 THEN data-'U' ELSE data END,
                    '{IA}',
                    to_jsonb(item_uuids)
                )::text,
                E'\n' ORDER BY num ASC) as data_agg
         FROM ehr_folder_data_history
         GROUP BY ehr_id, ehr_folders_idx, sys_version
     ) dh
WHERE (vh.ehr_id, vh.ehr_folders_idx, vh.sys_version)=(dh.ehr_id, dh.ehr_folders_idx, dh.sys_version);

DROP TABLE ehr_folder_data_history;
