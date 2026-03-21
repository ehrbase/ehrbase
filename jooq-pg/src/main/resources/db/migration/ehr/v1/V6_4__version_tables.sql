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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE INDEX comp_data_struc_idx
    ON comp_data USING btree
    (vo_id,
    rm_entity,
    entity_concept,
    entity_name collate "en_US",
    entity_idx)
    INCLUDE (entity_idx_cap, entity_idx_len, num);


CREATE INDEX comp_data_path_idx
    ON comp_data USING btree
    (vo_id,
    entity_attribute,
    entity_idx_len,
    rm_entity,
    entity_concept,
    entity_name collate "en_US"
    )
    INCLUDE (entity_idx,entity_idx_cap, num);


CREATE INDEX comp_version_ehr_idx
    ON comp_version USING btree
    (ehr_id, template_id)
    INCLUDE (vo_id,sys_version);


CREATE INDEX comp_version_template_idx
    ON comp_version USING btree
    (template_id)
    INCLUDE (vo_id,ehr_id,sys_version);


CREATE UNIQUE INDEX ehr_status_subject_idx ON ehr_status_data USING btree
    (jsonb_extract_path_text(data, VARIADIC ARRAY['su'::text, 'er'::text, 'X'::text, 'V'::text]),
     jsonb_extract_path_text(data, VARIADIC ARRAY['su'::text, 'er'::text, 'ns'::text]))
    INCLUDE (ehr_id)
    WHERE (num = 0);
