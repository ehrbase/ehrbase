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

--Composition
create table comp_version
(
    vo_id            uuid             NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,
    template_id      uuid             NOT NULL,

    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,

    PRIMARY KEY (vo_id)
    );

create table comp_version_history
(
    vo_id            uuid             NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,
    template_id      uuid             NOT NULL,

    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,
    sys_period_upper timestamptz,
    sys_deleted      boolean          NOT NULL,
    PRIMARY KEY (vo_id, sys_version)
    );

--EHR_STATUS
create table ehr_status_version
(
    vo_id            uuid             NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,

    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,

    PRIMARY KEY (ehr_id)
    );

create table ehr_status_version_history
(
    vo_id            uuid             NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,

    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,
    sys_period_upper timestamptz,
    sys_deleted      boolean          NOT NULL,
    PRIMARY KEY (ehr_id, sys_version)
    );

--EHR Folder
create table ehr_folder_version
(
    vo_id            uuid             NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,

    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,

    ehr_folders_idx  int4             NOT NULL,

    PRIMARY KEY (ehr_id, ehr_folders_idx)
    );

create table ehr_folder_version_history
(
    vo_id            uuid             NOT NULL,
    ehr_id           uuid             NOT NULL,
    contribution_id  uuid             NOT NULL,
    audit_id         uuid             NOT NULL,

    sys_version      INT              NOT NULL,
    sys_period_lower timestamptz      NOT NULL,

    ehr_folders_idx  int4             NOT NULL,

    sys_period_upper timestamptz,
    sys_deleted      boolean          NOT NULL,

    PRIMARY KEY (ehr_id, ehr_folders_idx, sys_version)
    );
