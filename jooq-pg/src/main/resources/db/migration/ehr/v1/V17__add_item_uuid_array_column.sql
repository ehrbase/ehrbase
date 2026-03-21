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

ALTER TABLE ehr_folder_data ADD IF NOT EXISTS item_uuids uuid[] NULL DEFAULT NULL;
ALTER TABLE ehr_folder_data_history ADD IF NOT EXISTS item_uuids uuid[] NULL DEFAULT NULL;

UPDATE ehr_folder_data
SET
    item_uuids = array(select (jsonb_array_elements(data -> 'i') -> 'X' ->> 'V')::uuid),
    data = data - 'i'
WHERE item_uuids is null;

UPDATE ehr_folder_data_history
SET
    item_uuids = array(select (jsonb_array_elements(data -> 'i') -> 'X' ->> 'V')::uuid),
    data = data - 'i'
WHERE item_uuids is null;

ALTER TABLE ehr_folder_data_history ALTER item_uuids DROP DEFAULT, ALTER item_uuids SET NOT NULL;
ALTER TABLE ehr_folder_data ALTER item_uuids DROP DEFAULT, ALTER item_uuids SET NOT NULL;
