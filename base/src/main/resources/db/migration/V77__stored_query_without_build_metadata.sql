/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

-- prohibit build metadata for semver

ALTER TABLE ehr.stored_query
    DISABLE ROW LEVEL SECURITY;

-- remove build metadata
UPDATE ehr.stored_query
SET semver = regexp_replace(semver, '^([^+]+)\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*)$', '\1')
WHERE semver ~ '\+';

ALTER TABLE ehr.stored_query
    DROP CONSTRAINT stored_query_semver_check;
-- prohibit build metadata
ALTER TABLE ehr.stored_query
    ADD CONSTRAINT stored_query_semver_check
        CHECK (semver ~*
               '^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)(?:-(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?$');

ALTER TABLE ehr.stored_query
    ENABLE ROW LEVEL SECURITY;