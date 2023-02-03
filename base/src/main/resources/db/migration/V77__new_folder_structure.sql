/*
 *  Copyright (c) 2021 Vitasystems GmbH.
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */


create table ehr.ehr_folder
(
    id                uuid,
    ehr_id            uuid        NOT NULL,
    contribution_id   uuid,
    archetype_node_id TEXT,
    path              TEXT[],
    contains          uuid[],
    fields            jsonb,
    namespace         TEXT default '1f332a66-0e57-11ed-861d-0242ac120002',
    sys_version       INT         NOT NULL,
    sys_period_lower  timestamptz NOT NULL,
    PRIMARY KEY (ehr_id, id),
    FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id),
    FOREIGN KEY (contribution_id) REFERENCES ehr.contribution (id)
);

create index folder2_path_idx ON ehr.ehr_folder USING btree ((path[2]), ehr_id);
create index archetype_node_idx ON ehr.ehr_folder USING btree (archetype_node_id, (path[2]), ehr_id);

ALTER TABLE ehr.ehr_folder
    ENABLE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr.ehr_folder FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));

create table ehr.ehr_folder_history
(
    id                uuid,
    ehr_id            uuid        NOT NULL,
    contribution_id   uuid,
    archetype_node_id TEXT,
    path              TEXT[],
    contains          uuid[],
    fields            jsonb,
    namespace         TEXT default '1f332a66-0e57-11ed-861d-0242ac120002',
    sys_version       INT         NOT NULL,
    sys_period_lower  timestamptz NOT NULL,
    sys_period_upper  timestamptz,
    sys_deleted       boolean     NOT NULL,
    PRIMARY KEY (ehr_id, id, sys_version),
    FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id),
    FOREIGN KEY (contribution_id) REFERENCES ehr.contribution (id)
);

ALTER TABLE ehr.ehr_folder_history
    ENABLE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr.ehr_folder_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));