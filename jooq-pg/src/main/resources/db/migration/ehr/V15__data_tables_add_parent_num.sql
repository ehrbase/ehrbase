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
 * See the License for the specific LANGUAGE governing permissions and
 * limitations under the License.
 */

ALTER TABLE comp_data
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE comp_data_history
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE ehr_status_data
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE ehr_status_data_history
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE ehr_folder_data
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE ehr_folder_data_history
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;

DO $$ BEGIN
    if exists (
        select from pg_proc p, pg_namespace ns where ns.nspname = 'ext' and ns.oid = p.pronamespace and proname = 'mig_num_columns'
    ) then
        CALL ext.mig_num_columns('ehr.ehr_folder_data_history'::regclass, 'ehr_id', 'mig_ehr_folder_data_history_num_idx',1000);
        CALL ext.mig_num_columns('ehr.ehr_folder_data'::regclass, 'ehr_id', 'mig_ehr_folder_data_num_idx',1000);
        CALL ext.mig_num_columns('ehr.ehr_status_data_history'::regclass, 'ehr_id', 'mig_ehr_status_data_history_num_idx',1000);
        CALL ext.mig_num_columns('ehr.ehr_status_data'::regclass, 'ehr_id', 'mig_ehr_status_data_num_idx',1000);
        CALL ext.mig_num_columns('ehr.comp_data_history'::regclass, 'vo_id', 'mig_comp_data_history_num_idx',1000);
        CALL ext.mig_num_columns('ehr.comp_data'::regclass, 'vo_id', 'mig_comp_data_num_idx',1000);

        DROP PROCEDURE ext.mig_num_columns;
        DROP FUNCTION IF EXISTS ext.mig_retrieve_nums_batch;
        DROP FUNCTION IF EXISTS ext.mig_calc_nums;
        DROP TYPE IF EXISTS ext.mig_num_type;

    else
        --migrate compositions
        UPDATE comp_data ch SET parent_num=pa.num
        FROM comp_data pa
        WHERE ch.vo_id=pa.vo_id
          AND pa.entity_idx_len > 1
          AND pa.entity_idx_len = ch.entity_idx_len - 1
          AND ch.entity_idx ^@ pa.entity_idx
          AND ch.parent_num = 0;
        UPDATE comp_data_history ch SET parent_num=pa.num
        FROM comp_data_history pa
        WHERE ch.vo_id=pa.vo_id
          AND ch.sys_version=pa.sys_version
          AND pa.entity_idx_len > 1
          AND ch.sys_version=pa.sys_version
          AND pa.entity_idx_len = ch.entity_idx_len - 1
          AND ch.entity_idx ^@ pa.entity_idx
          AND ch.parent_num = 0;

        --migrate ehr_status
        UPDATE ehr_status_data ch SET parent_num=pa.num
        FROM ehr_status_data pa
        WHERE ch.ehr_id=pa.ehr_id
          AND pa.entity_idx_len != 0
          AND pa.entity_idx_len = ch.entity_idx_len - 1
          AND ch.entity_idx ^@ pa.entity_idx
          AND ch.parent_num = 0;
        UPDATE ehr_status_data_history ch SET parent_num=pa.num
        FROM ehr_status_data_history pa
        WHERE ch.ehr_id=pa.ehr_id
          AND ch.sys_version=pa.sys_version
          AND pa.entity_idx_len != 0
          AND ch.sys_version=pa.sys_version
          AND pa.entity_idx_len = ch.entity_idx_len - 1
          AND ch.entity_idx ^@ pa.entity_idx
          AND ch.parent_num = 0;

        --migrate ehr_folder
        UPDATE ehr_folder_data ch SET parent_num=pa.num
        FROM ehr_folder_data pa
        WHERE ch.ehr_id=pa.ehr_id
          AND pa.entity_idx_len != 0
          AND pa.entity_idx_len = ch.entity_idx_len - 1
          AND ch.entity_idx ^@ pa.entity_idx
          AND ch.parent_num = 0;
        UPDATE ehr_folder_data_history ch SET parent_num=pa.num
        FROM ehr_folder_data_history pa
        WHERE ch.ehr_id=pa.ehr_id
          AND ch.sys_version=pa.sys_version
          AND pa.entity_idx_len != 0
          AND ch.sys_version=pa.sys_version
          AND pa.entity_idx_len = ch.entity_idx_len - 1
          AND ch.entity_idx ^@ pa.entity_idx
          AND ch.parent_num = 0;

        -- num cap
        UPDATE comp_data pa SET num_cap = (
            select max(ch.num) FROM comp_data ch
            WHERE ch.vo_id=pa.vo_id
              AND pa.entity_idx_len >= ch.entity_idx_len
              AND ch.entity_idx ^@ pa.entity_idx
        ) WHERE pa.num_cap = -1;
        UPDATE comp_data_history pa SET num_cap = (
            select max(ch.num) FROM comp_data_history ch
            WHERE ch.vo_id=pa.vo_id
              AND ch.sys_version=pa.sys_version
              AND pa.entity_idx_len >= ch.entity_idx_len
              AND ch.entity_idx ^@ pa.entity_idx
        ) WHERE pa.num_cap = -1;
        UPDATE ehr_status_data pa SET num_cap = (
            select max(ch.num) FROM ehr_status_data ch
            WHERE ch.ehr_id=pa.ehr_id
              AND pa.entity_idx_len >= ch.entity_idx_len
              AND ch.entity_idx ^@ pa.entity_idx
        ) WHERE pa.num_cap = -1;
        UPDATE ehr_status_data_history pa SET num_cap = (
            select max(ch.num) FROM ehr_status_data_history ch
            WHERE ch.ehr_id=pa.ehr_id
              AND ch.sys_version=pa.sys_version
              AND pa.entity_idx_len >= ch.entity_idx_len
              AND ch.entity_idx ^@ pa.entity_idx
        ) WHERE pa.num_cap = -1;
        UPDATE ehr_folder_data pa SET num_cap = (
            select max(ch.num) FROM ehr_folder_data ch
            WHERE ch.ehr_id=pa.ehr_id
              AND pa.entity_idx_len >= ch.entity_idx_len
              AND ch.entity_idx ^@ pa.entity_idx
        ) WHERE pa.num_cap = -1;
        UPDATE ehr_folder_data_history pa SET num_cap = (
            select max(ch.num)
             FROM ehr_folder_data_history ch
             WHERE ch.ehr_id=pa.ehr_id
               AND ch.sys_version=pa.sys_version
               AND pa.entity_idx_len >= ch.entity_idx_len
               AND ch.entity_idx ^@ pa.entity_idx
        ) WHERE pa.num_cap = -1;
    end if;

end $$;

DROP INDEX IF EXISTS mig_ehr_folder_data_history_num_idx;
DROP INDEX IF EXISTS mig_ehr_folder_data_num_idx;
DROP INDEX IF EXISTS mig_ehr_status_data_history_num_idx;
DROP INDEX IF EXISTS mig_ehr_status_data_num_idx;
DROP INDEX IF EXISTS mig_comp_data_history_num_idx;
DROP INDEX IF EXISTS mig_comp_data_num_idx;

DROP INDEX IF EXISTS comp_data_idx;
DROP INDEX IF EXISTS comp_data_leaf_idx;
DROP INDEX IF EXISTS ehr_status_data_idx;

-- drop defaults and columns (implies dropping indexes)
ALTER TABLE comp_data
    ALTER COLUMN parent_num DROP DEFAULT,
    ALTER COLUMN num_cap DROP DEFAULT,
    DROP COLUMN IF EXISTS entity_idx_cap,
    DROP COLUMN IF EXISTS entity_path,
    DROP COLUMN IF EXISTS entity_path_cap;
ALTER TABLE comp_data_history
    ALTER COLUMN parent_num DROP DEFAULT,
    ALTER COLUMN num_cap DROP DEFAULT,
    DROP COLUMN IF EXISTS entity_idx_cap,
    DROP COLUMN IF EXISTS entity_path,
    DROP COLUMN IF EXISTS entity_path_cap;
ALTER TABLE ehr_status_data
    ALTER COLUMN parent_num DROP DEFAULT,
    ALTER COLUMN num_cap DROP DEFAULT,
    DROP COLUMN IF EXISTS entity_idx_cap,
    DROP COLUMN IF EXISTS entity_path,
    DROP COLUMN IF EXISTS entity_path_cap;
ALTER TABLE ehr_status_data_history
    ALTER COLUMN parent_num DROP DEFAULT,
    ALTER COLUMN num_cap DROP DEFAULT,
    DROP COLUMN IF EXISTS entity_idx_cap,
    DROP COLUMN IF EXISTS entity_path,
    DROP COLUMN IF EXISTS entity_path_cap;
ALTER TABLE ehr_folder_data
    ALTER COLUMN parent_num DROP DEFAULT,
    ALTER COLUMN num_cap DROP DEFAULT,
    DROP COLUMN IF EXISTS entity_idx_cap,
    DROP COLUMN IF EXISTS entity_path,
    DROP COLUMN IF EXISTS entity_path_cap;
ALTER TABLE ehr_folder_data_history
    ALTER COLUMN parent_num DROP DEFAULT,
    ALTER COLUMN num_cap DROP DEFAULT,
    DROP COLUMN IF EXISTS entity_idx_cap,
    DROP COLUMN IF EXISTS entity_path,
    DROP COLUMN IF EXISTS entity_path_cap;

create index IF NOT EXISTS ehr_status_data_path_idx on ehr_status_data
    (ehr_id, parent_num, entity_attribute, entity_concept, rm_entity, num, num_cap);
create index IF NOT EXISTS comp_data_path_idx on comp_data
    (vo_id, parent_num, entity_attribute, entity_concept, rm_entity, num, num_cap);
