/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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

ALTER TABLE ehr.entry
    DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry_history
    DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context
    DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context_history
    DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.status
    DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.status_history
    DISABLE ROW LEVEL SECURITY;

--composition content
UPDATE ehr.entry
SET entry=REPLACE(REPLACE(entry::text,'"codeString":', '"code_string":'),'"terminologyId":', '"terminology_id":')::jsonb;

UPDATE ehr.entry_history
SET entry=REPLACE(REPLACE(entry::text,'"codeString":', '"code_string":'),'"terminologyId":', '"terminology_id":')::jsonb
WHERE entry IS NOT NULL;

--event_context.other_context
UPDATE ehr.event_context
SET other_context=REPLACE(REPLACE(other_context::text,'"codeString":', '"code_string":'),'"terminologyId":', '"terminology_id":')::jsonb
WHERE other_context IS NOT NULL;

UPDATE ehr.event_context_history
SET other_context=REPLACE(REPLACE(other_context::text,'"codeString":', '"code_string":'),'"terminologyId":', '"terminology_id":')::jsonb
WHERE other_context IS NOT NULL;

--status.other_details
UPDATE ehr.status
SET other_details=REPLACE(REPLACE(other_details::text,'"codeString":', '"code_string":'),'"terminologyId":', '"terminology_id":')::jsonb
WHERE other_details IS NOT NULL;

UPDATE ehr.status_history
SET other_details=REPLACE(REPLACE(other_details::text,'"codeString":', '"code_string":'),'"terminologyId":', '"terminology_id":')::jsonb
WHERE other_details IS NOT NULL;

ALTER TABLE ehr.entry
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry_history
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context_history
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.status
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.status_history
    ENABLE ROW LEVEL SECURITY;
