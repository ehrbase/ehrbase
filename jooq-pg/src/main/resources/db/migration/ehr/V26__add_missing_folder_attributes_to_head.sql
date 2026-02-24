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


UPDATE ehr_folder_data
SET data = data ||
           (CASE
               WHEN entity_concept IS NULL THEN
                   '{"ad":{"T":"AR","rv":"1.0.4","aX": {"T":"AX","V": "openEHR-EHR-FOLDER.generic.v1"}},"A":"openEHR-EHR-FOLDER.generic.v1"}'
               ELSE
                   '{"ad":{"T":"AR","rv":"1.0.4","aX": {"T":"AX","V": "' || (data ->> 'A') || '"}}}'
               END)::jsonb,
    entity_concept =
        CASE
        WHEN entity_concept IS NULL THEN
            '.generic.v1'
        ELSE
            entity_concept
        END
WHERE (entity_concept IS NULL AND rm_entity='F') OR (starts_with(entity_concept, '.') AND NOT data ? 'ad');
