/*
 * Copyright (c) 2025 vitasystems GmbH.
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

DROP INDEX comp_data_path_idx;
CREATE INDEX comp_data_path_idx
    ON ehr.comp_data USING btree
        (vo_id, parent_num, entity_concept)
    INCLUDE(rm_entity, entity_attribute, entity_name, num, num_cap, citem_num, entity_idx);

CREATE INDEX IF NOT EXISTS comp_data_path_skip_idx
    ON comp_data USING btree (vo_id, citem_num, num)
    INCLUDE(entity_concept, rm_entity, entity_attribute, parent_num, num_cap, entity_idx);
