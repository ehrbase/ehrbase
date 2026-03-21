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

DROP INDEX IF EXISTS ehr_status_subject_idx;

ALTER INDEX IF EXISTS prep_ehr_status_subject_idx RENAME TO ehr_status_subject_idx;

CREATE UNIQUE INDEX IF NOT EXISTS ehr_status_subject_idx ON ehr_status_data USING btree
    ((data -> 'su' -> 'er' -> 'X' -> 'V' ->> 0),
     (data -> 'su' -> 'er' -> 'ns' ->> 0))
    INCLUDE (ehr_id, num)
    WHERE (rm_entity = 'ES');
