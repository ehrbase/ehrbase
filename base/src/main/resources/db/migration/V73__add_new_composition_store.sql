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

create table ehr.entry2
(
    ehr_id         uuid,
    comp_id        uuid,
    num            INTEGER,
    entity_concept Text,
    rm_entity      text,
    entity_path    Text,
    entity_idx     INTEGER[],
    field_idx      INTEGER[],
    field_idx_len  INTEGER,
    fields         jsonb,
    PRIMARY KEY (comp_id, num),
    FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id)
);

create index archetype_idx on ehr.entry2 (entity_concept, field_idx_len, ehr_id);

create index type_idx on ehr.entry2 (rm_entity, field_idx_len, ehr_id);

