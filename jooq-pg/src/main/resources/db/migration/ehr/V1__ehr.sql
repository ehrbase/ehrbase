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

create sequence tenant_id_seq;


create type contribution_data_type as enum ('composition', 'folder', 'ehr', 'system', 'other');
create type contribution_state as enum ('complete', 'incomplete', 'deleted');
create type contribution_change_type as enum ('creation', 'amendment', 'modification', 'synthesis', 'Unknown', 'deleted');

create table plugin
(
    id       uuid default uuid_generate_v4() not null
        constraint plugin_pkey
            primary key,
    pluginid text                            not null,
    key      text                            not null,
    value    text
);

comment on table plugin is 'key value store for plugin sub system';

create table system
(
    id          uuid default uuid_generate_v4() not null
        constraint system_pkey
            primary key,
    description text                            not null,
    settings    text                            not null
        constraint ehr_system_settings_idx
            unique
);

comment on table system is 'system table for reference';

create table tenant
(
    id                int2 default nextval('tenant_id_seq'::regclass) not null
        constraint tenant_pkey
            primary key,
    tenant_id         text
        constraint tenant_tenant_id_key
            unique,
    tenant_properties jsonb,
    tenant_name       text
        constraint tenant_tenant_name_key
            unique
);

create table ehr
(
    id            uuid not null,
    sys_tenant    int2 not null
        constraint ehr_sys_tenant_fkey
            references tenant,
    creation_date timestamptz(6),
    constraint ehr_pkey
        primary key (id, sys_tenant
)
    );

ALTER TABLE ehr
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

create table users
(
    id         uuid not null
        constraint users_pkey
            primary key,
    username   text not null,
    sys_tenant int2 not null
        constraint users_sys_tenant_fkey
            references tenant
);

CREATE UNIQUE INDEX users_username_idx on users (username, sys_tenant);

ALTER TABLE users
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON users FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);


create table audit_details
(
    id             uuid                       not null,
    system_id      uuid                       not null
        constraint audit_details_system_id_fkey
            references system,
    change_type    "contribution_change_type" not null,
    description    text,
    sys_tenant     int2                       not null
        constraint audit_details_sys_tenant_fkey
            references tenant,
    time_committed timestamptz(6)             not null,
    committer      jsonb,
    user_id        uuid                       not null
        constraint audit_details_user_id_fkey
            references users,
    constraint audit_details_pkey
        primary key (id, sys_tenant
)
    );

ALTER TABLE audit_details
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON audit_details FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);


create table contribution
(
    id                uuid not null,
    ehr_id            uuid,
    contribution_type "contribution_data_type",
    state             "contribution_state",
    signature         text,
    has_audit         uuid,
    sys_tenant        int2 not null
        constraint contribution_sys_tenant_fkey
            references tenant,
    constraint contribution_pkey
        primary key (id, sys_tenant
) ,
    constraint contribution_ehr_id_fkey
        foreign key (ehr_id, sys_tenant) references ehr (id, sys_tenant)
            on delete cascade,
    constraint contribution_has_audit_fkey
        foreign key (has_audit, sys_tenant) references audit_details
            on delete cascade
);

create index contribution_ehr_idx
    on contribution (ehr_id, sys_tenant);

ALTER TABLE contribution
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON contribution FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

create table stored_query
(
    reverse_domain_name varchar                 not null,
    semantic_id         varchar                 not null,
    semver              varchar default '0.0.0' not null,
    query_text          varchar                 not null,
    type                varchar default 'AQL',
    sys_tenant          int2                    not null
        constraint stored_query_sys_tenant_fkey
            references tenant,
    creation_date       timestamptz(6)          not null,
    constraint stored_query_pkey
        primary key (reverse_domain_name, semantic_id, sys_tenant,
    semver
)
    );

ALTER TABLE stored_query
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON stored_query FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

create table template_store
(
    id            uuid           not null,
    template_id   text           not null,
    content       text,
    sys_tenant    int2           not null
        constraint template_store_sys_tenant_fkey
            references tenant,
    creation_time timestamptz(6) not null,
    constraint template_store_pkey
        primary key (id, sys_tenant
)
    );

CREATE UNIQUE INDEX template_store_id_unq on template_store (template_id, sys_tenant);

ALTER TABLE template_store
    ENABLE ROW LEVEL SECURITY,
    FORCE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON template_store FOR ALL USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);
