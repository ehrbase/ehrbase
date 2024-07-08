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

ALTER TABLE comp_data ADD COLUMN parent_num integer NOT NULL DEFAULT 0;
ALTER TABLE comp_data_history ADD COLUMN parent_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_status_data ADD COLUMN parent_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_status_data_history ADD COLUMN parent_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_folder_data ADD COLUMN parent_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_folder_data_history ADD COLUMN parent_num integer NOT NULL DEFAULT 0;


ALTER TABLE comp_data ADD COLUMN max_child_num integer NOT NULL DEFAULT 0;
ALTER TABLE comp_data_history ADD COLUMN max_child_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_status_data ADD COLUMN max_child_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_status_data_history ADD COLUMN max_child_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_folder_data ADD COLUMN max_child_num integer NOT NULL DEFAULT 0;
ALTER TABLE ehr_folder_data_history ADD COLUMN max_child_num integer NOT NULL DEFAULT 0;

--migrate compositions
UPDATE comp_data ch SET parent_num=pa.num
FROM comp_data pa
WHERE ch.vo_id=pa.vo_id
  AND pa.entity_idx_len = ch.entity_idx_len - 1
  AND ch.entity_idx LIKE (pa.entity_idx || '%');
UPDATE comp_data_history ch SET parent_num=pa.num
FROM comp_data_history pa
WHERE ch.vo_id=pa.vo_id
  AND ch.sys_version=pa.sys_version
  AND pa.entity_idx_len = ch.entity_idx_len - 1
  AND ch.entity_idx LIKE (pa.entity_idx || '%');

--migrate ehr_status
UPDATE ehr_status_data ch SET parent_num=pa.num
FROM ehr_status_data pa
WHERE ch.vo_id=pa.vo_id
  AND pa.entity_idx_len = ch.entity_idx_len - 1
  AND ch.entity_idx LIKE (pa.entity_idx || '%');
UPDATE ehr_status_data_history ch SET parent_num=pa.num
FROM ehr_status_data_history pa
WHERE ch.vo_id=pa.vo_id
  AND ch.sys_version=pa.sys_version
  AND pa.entity_idx_len = ch.entity_idx_len - 1
  AND ch.entity_idx LIKE (pa.entity_idx || '%');

--migrate ehr_folder
--TODO ehr_folder_data(_history)

--TODO max child num


ALTER TABLE comp_data ALTER COLUMN parent_num DROP DEFAULT;
ALTER TABLE comp_data_history ALTER COLUMN parent_num DROP DEFAULT;
ALTER TABLE ehr_status_data ALTER COLUMN parent_num DROP DEFAULT;
ALTER TABLE ehr_status_data_history ALTER COLUMN parent_num DROP DEFAULT;
ALTER TABLE ehr_folder_data ALTER COLUMN parent_num DROP DEFAULT;
ALTER TABLE ehr_folder_data_history ALTER COLUMN parent_num DROP DEFAULT;

ALTER TABLE comp_data ALTER COLUMN max_child_num DROP DEFAULT;
ALTER TABLE comp_data_history ALTER COLUMN max_child_num DROP DEFAULT;
ALTER TABLE ehr_status_data ALTER COLUMN max_child_num DROP DEFAULT;
ALTER TABLE ehr_status_data_history ALTER COLUMN max_child_num DROP DEFAULT;
ALTER TABLE ehr_folder_data ALTER COLUMN max_child_num DROP DEFAULT;
ALTER TABLE ehr_folder_data_history ALTER COLUMN max_child_num DROP DEFAULT;
