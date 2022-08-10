/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


ALTER TABLE ehr.ehr ADD namespace TEXT;

ALTER TABLE ehr.entry ADD namespace TEXT;
ALTER TABLE ehr.entry_history ADD namespace TEXT;

ALTER TABLE ehr.composition ADD namespace TEXT;
ALTER TABLE ehr.composition_history ADD namespace TEXT;

ALTER TABLE ehr.event_context ADD namespace TEXT;
ALTER TABLE ehr.event_context_history ADD namespace TEXT;

-- insert dummy tenant

UPDATE ehr.ehr SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;

UPDATE ehr.entry SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.entry_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;

UPDATE ehr.composition SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.composition_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;

UPDATE ehr.event_context SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.event_context_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;

