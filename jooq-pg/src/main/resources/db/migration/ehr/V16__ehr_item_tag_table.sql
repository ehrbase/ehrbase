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

--RM type: COMPOSITION, EHR_STATUS
create type ehr_item_tag_target_type as enum ('ehr_status', 'composition');

--One single table for all VERSIONED_OBJECT types
CREATE TABLE ehr_item_tag
(
    id               uuid                       NOT NULL,
    ehr_id           uuid                       NOT NULL,
    target_vo_id     uuid                       NOT NULL,
    target_type      "ehr_item_tag_target_type" NOT NULL,
    key              text collate "C"           NOT NULL,
    value            text collate "C",
    target_path      text collate "C",
    creation_date    timestamptz(6)             NOT NULL,
    sys_period_lower timestamptz(6)             NOT NULL,

    CONSTRAINT ehr_item_tag_pkey PRIMARY KEY (id),
    CONSTRAINT ehr_item_tag_ehr_id_fkey FOREIGN KEY (ehr_id) REFERENCES ehr (id)
);

-- Index on ehr_id, target_vo_id
CREATE INDEX ehr_item_tag_ehr_id_target_vo_id_idx ON ehr_item_tag (ehr_id, target_vo_id);
