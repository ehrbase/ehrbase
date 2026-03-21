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

create table ehr_status
(
    vo_id            uuid           NOT NULL,
    num              int4           NOT NULL,
    ehr_id           uuid           NOT NULL,
    contribution_id  uuid           NOT NULL,
    audit_id         uuid           NOT NULL,
    citem_num        int4,
    rm_entity        text           NOT NULL,
    entity_concept   text,
    entity_name      text collate "en_US",
    entity_attribute text,
    entity_path      text collate "C" NOT NULL,
    entity_path_cap  text collate "C" NOT NULL,
    entity_idx       text collate "C" NOT NULL,
    entity_idx_cap   text collate "C" NOT NULL,
    entity_idx_len   int4           NOT NULL,
    data             jsonb          NOT NULL,
    sys_tenant       int2           NOT NULL,
    sys_version      int4           NOT NULL,
    sys_period_lower timestamptz(6) NOT NULL,

    PRIMARY KEY (ehr_id, sys_tenant,num),
    FOREIGN KEY (audit_id, sys_tenant) references audit_details (id, sys_tenant),
    FOREIGN KEY (contribution_id, sys_tenant) references contribution (id, sys_tenant),
    FOREIGN KEY (ehr_id, sys_tenant) references ehr (id, sys_tenant),
    FOREIGN KEY (sys_tenant) references tenant (id)
);

create unique index ehr_status_subject on ehr_status (
  jsonb_extract_path_text("ehr_status"."data", 'su', 'er', 'X', 'V'),
  jsonb_extract_path_text("ehr_status"."data", 'su', 'er', 'ns'),
  sys_tenant
) INCLUDE (ehr_id)
where num = 0;


ALTER TABLE ehr_status
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr_status FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

create table ehr_status_history
(
    vo_id            uuid           NOT NULL,
    num              int4           NOT NULL,
    ehr_id           uuid           NOT NULL,
    contribution_id  uuid           NOT NULL,
    audit_id         uuid           NOT NULL,
    citem_num        int4,
    rm_entity        text           NOT NULL,
    entity_concept   text,
    entity_name      text collate "en_US",
    entity_attribute text,
    entity_path      text collate "C" NOT NULL,
    entity_path_cap  text collate "C" NOT NULL,
    entity_idx       text collate "C" NOT NULL,
    entity_idx_cap   text collate "C" NOT NULL,
    entity_idx_len   int4           NOT NULL,
    data             jsonb          NOT NULL,
    sys_tenant       int2           NOT NULL,
    sys_version      int4           NOT NULL,
    sys_period_lower timestamptz(6) NOT NULL,
    sys_period_upper timestamptz(6),
    sys_deleted      bool           NOT NULL,

    PRIMARY KEY (ehr_id, sys_tenant,num,sys_version),
    FOREIGN KEY (audit_id, sys_tenant) references audit_details (id, sys_tenant),
    FOREIGN KEY (contribution_id, sys_tenant) references contribution (id, sys_tenant),
    FOREIGN KEY (ehr_id, sys_tenant) references ehr (id, sys_tenant),
    FOREIGN KEY (sys_tenant) references tenant(id)
);

ALTER TABLE ehr_status_history
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr_status_history FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

create table ehr_folder
(
    vo_id            uuid           NOT NULL,
    num              int4           NOT NULL,
    ehr_id           uuid           NOT NULL,
    ehr_folders_idx  int4          NOT NULL,
    contribution_id  uuid           NOT NULL,
    audit_id         uuid           NOT NULL,
    citem_num        int4,
    rm_entity        text           NOT NULL,
    entity_concept   text,
    entity_name      text collate "en_US",
    entity_attribute text,
    entity_path      text collate "C" NOT NULL,
    entity_path_cap  text collate "C" NOT NULL,
    entity_idx       text collate "C" NOT NULL,
    entity_idx_cap   text collate "C" NOT NULL,
    entity_idx_len   int4           NOT NULL,
    data             jsonb          NOT NULL,
    sys_tenant       int2           NOT NULL,
    sys_version      int4           NOT NULL,
    sys_period_lower timestamptz(6) NOT NULL,

    PRIMARY KEY (ehr_id,ehr_folders_idx, sys_tenant, num),
    FOREIGN KEY (audit_id, sys_tenant) references audit_details (id, sys_tenant),
    FOREIGN KEY (contribution_id, sys_tenant) references contribution (id, sys_tenant),
    FOREIGN KEY (ehr_id, sys_tenant) references ehr (id, sys_tenant),
    FOREIGN KEY (sys_tenant) references tenant (id)
);

ALTER TABLE ehr_folder
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr_folder FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);


create table ehr_folder_history
(
    vo_id            uuid           NOT NULL,
    num              int4           NOT NULL,
    ehr_id           uuid           NOT NULL,
    ehr_folders_idx  int4           NOT NULL,
    contribution_id  uuid           NOT NULL,
    audit_id         uuid           NOT NULL,
    citem_num        int4,
    rm_entity        text           NOT NULL,
    entity_concept   text,
    entity_name      text collate "en_US",
    entity_attribute text,
    entity_path      text collate "C" NOT NULL,
    entity_path_cap  text collate "C" NOT NULL,
    entity_idx       text collate "C" NOT NULL,
    entity_idx_cap   text collate "C" NOT NULL,
    entity_idx_len   int4           NOT NULL,
    data             jsonb          NOT NULL,
    sys_tenant       int2           NOT NULL,
    sys_version      int4           NOT NULL,
    sys_period_lower timestamptz(6) NOT NULL,
    sys_period_upper timestamptz(6),
    sys_deleted      bool           NOT NULL,

    PRIMARY KEY (ehr_id,ehr_folders_idx, sys_tenant, num, sys_version),
    FOREIGN KEY (audit_id, sys_tenant) references audit_details (id, sys_tenant),
    FOREIGN KEY (contribution_id, sys_tenant) references contribution (id, sys_tenant),
    FOREIGN KEY (ehr_id, sys_tenant) references ehr (id, sys_tenant),
    FOREIGN KEY (sys_tenant) references tenant(id)
);

ALTER TABLE ehr_folder_history
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr_folder_history FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

create table comp
(
    vo_id            uuid             NOT NULL,
    num              integer          NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,
    template_id      uuid             NOT NULL,
    citem_num        int4,
    rm_entity        text             NOT NULL,
    entity_concept   text,
    entity_name      text collate "en_US",
    entity_attribute text,
    entity_path      text collate "C" NOT NULL,
    entity_path_cap  text collate "C" NOT NULL,
    entity_idx       text collate "C" NOT NULL,
    entity_idx_cap   text collate "C" NOT NULL,
    --for querying direct children
    entity_idx_len   INTEGER          NOT NULL,

    --data...

    data             jsonb            NOT NULL,

    sys_tenant       SMALLINT         NOT NULL,
    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,

    PRIMARY KEY (vo_id, sys_tenant, num),
    FOREIGN KEY (ehr_id, sys_tenant) REFERENCES ehr (id, sys_tenant),
    FOREIGN KEY (contribution_id, sys_tenant) REFERENCES contribution (id, sys_tenant),
    FOREIGN KEY (audit_id, sys_tenant) REFERENCES audit_details (id, sys_tenant),
    FOREIGN KEY (template_id, sys_tenant) REFERENCES template_store (id, sys_tenant),
    FOREIGN KEY (sys_tenant) REFERENCES tenant (id)
);

ALTER TABLE comp
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON comp FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);


create table comp_history
(
    vo_id            uuid             NOT NULL,
    num              integer          NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,
    template_id      uuid             NOT NULL,
    citem_num        int4,
    rm_entity        text             NOT NULL,
    entity_concept   text,
    entity_name      text collate "en_US",
    entity_attribute text,
    entity_path      text collate "C" NOT NULL,
    entity_path_cap  text collate "C" NOT NULL,
    entity_idx       text collate "C" NOT NULL,
    entity_idx_cap   text collate "C" NOT NULL,
    --for querying direct children
    entity_idx_len   INTEGER          NOT NULL,

    --data...
    data             jsonb            NOT NULL,

    sys_tenant       smallint         NOT NULL,
    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,
    sys_period_upper timestamptz,
    sys_deleted      boolean          NOT NULL,
    PRIMARY KEY (vo_id, sys_tenant, num, sys_version),
    FOREIGN KEY (ehr_id, sys_tenant) REFERENCES ehr (id, sys_tenant),
    FOREIGN KEY (contribution_id, sys_tenant) REFERENCES contribution (id, sys_tenant),
    FOREIGN KEY (audit_id, sys_tenant) REFERENCES audit_details (id, sys_tenant),
    FOREIGN KEY (template_id, sys_tenant) REFERENCES template_store (id, sys_tenant),
    FOREIGN KEY (sys_tenant) REFERENCES tenant (id)
);

ALTER TABLE comp_history
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON comp_history FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

CREATE INDEX comp_struc_ehr_idx
    ON comp USING btree
    (sys_tenant, ehr_id,
    vo_id,
    rm_entity,
    entity_idx,
    entity_concept,
    entity_name collate "en_US")
    INCLUDE (entity_idx_cap, num, sys_version);

CREATE INDEX comp_struc_idx ON comp
    (sys_tenant, rm_entity,
    vo_id,
    entity_idx,
    entity_concept,
    entity_name collate "en_US")
    INCLUDE(ehr_id, entity_idx_cap, num, sys_version);

CREATE INDEX comp_data_idx
    ON comp USING btree
    (sys_tenant, vo_id);
