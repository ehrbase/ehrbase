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

--
-- Structure holding the parent and cap numbers as well as query relevant identifier
--
DROP TYPE IF EXISTS ext.mig_num_type CASCADE;
CREATE TYPE ext.mig_num_type AS
(
    vo_id uuid,
    num integer,
    ehr_id uuid,
    entity_idx_len integer,
    sys_version integer,
    parent_num integer,
    num_cap integer
);

--
-- Calculates the `parent_num` and `num_cap` for the given `ext.mig_num_type`s
--
CREATE OR REPLACE FUNCTION ext.mig_calc_nums(rs ext.mig_num_type[])
    RETURNS ext.mig_num_type[]
    LANGUAGE plpgsql
    IMMUTABLE PARALLEL SAFE STRICT
AS $function$
DECLARE
    r ext.mig_num_type;
    prev_r ext.mig_num_type;
    h int[];
    i int;
    j int;
BEGIN
    --parent_num
    h[-1] = 0;
    FOR i IN array_lower(rs, 1) .. array_upper(rs, 1)
        LOOP
            r = rs[i];
            h[r.entity_idx_len] = r.num;
            r.parent_num = h[r.entity_idx_len - 1];
            rs[i] = r;
        END LOOP;
    -- num_cap
    FOR i IN REVERSE array_upper(rs, 1) .. array_lower(rs, 1)
        LOOP
            r = rs[i];
            IF prev_r is null or r.vo_id != prev_r.vo_id or r.sys_version != prev_r.sys_version THEN
                --last leaf
                r.num_cap = r.num;
                for j in 0 .. r.entity_idx_len LOOP
                        h[j] = r.num;
                    END LOOP;
            ELSEIF  r.entity_idx_len = prev_r.entity_idx_len THEN
                -- sibling
                r.num_cap = r.num;
                h[r.entity_idx_len] = r.num;
            ELSEIF  r.entity_idx_len < prev_r.entity_idx_len THEN
                --parent
                r.num_cap = h[r.entity_idx_len];
            ELSE -- r.entity_idx_len > prev_r.entity_idx_len
            --different ancestor
                r.num_cap = r.num;
                for j in prev_r.entity_idx_len .. r.entity_idx_len LOOP
                        h[j] = r.num;
                    END LOOP;
            END IF;
            rs[i] = r;
            prev_r = r;
        END LOOP;
    return rs;
END;$function$;

--
-- Select structures from affected table
--
CREATE OR REPLACE FUNCTION ext.mig_retrieve_nums_batch(
    table_name text,
    id_exp text,
    ehr_id_exp text,
    version_exp text,
    batch_size integer
) RETURNS ext.mig_num_type[]
    LANGUAGE plpgsql
    IMMUTABLE STRICT
AS $function$
DECLARE
    rs ext.mig_num_type[];
BEGIN
    EXECUTE format('
        SELECT array_agg(sub.r)
        FROM(
            SELECT row(
                VO_ID,
                NUM,
                %4$s,           --EHR_ID
                ENTITY_IDX_LEN,
                %2$s,           --SYS_VERSION
                0,              --parent_num
                -1              --num_cap
            ) as r
            FROM (
               SELECT %1$s AS id
               FROM %3$s v
               WHERE num = 0 and num_cap = -1 and %2$s = 1
               LIMIT $1
            ) ids
            JOIN %3$s d ON ids.id = d.%1$s
            ORDER BY ids.id, %2$s, d.num
        ) sub', id_exp, version_exp, table_name, ehr_id_exp)
        INTO rs
        USING batch_size;
    return rs;
END;$function$;

--
-- Migration procedure
--
CREATE OR REPLACE PROCEDURE ext.mig_num_columns(
    rel_name regclass,
    id_exp text,
    batch_size integer DEFAULT 1000
) LANGUAGE plpgsql
AS $procedure$
DECLARE
    rs ext.mig_num_type[];
    cols text;
    ehr_id_exp text;
    version_exp text;
    updated_cnt integer := -1;
    err_msg text;
    err_state text;
    last timestamp;
BEGIN

    RAISE NOTICE 'Starting migration for %', rel_name;

    SELECT string_agg(attname, ',')
    FROM (
             SELECT attname
             FROM pg_attribute
             WHERE attrelid=rel_name AND NOT attisdropped AND attnum > 0
             ORDER BY attnum
         ) att
    INTO cols;

    ehr_id_exp=COALESCE(substring(cols FROM '(?i)ehr_id'),'NULL');
    version_exp=COALESCE(substring(cols FROM '(?i)sys_version'),'1::int');

    last = clock_timestamp();

    WHILE updated_cnt <> 0 LOOP
            BEGIN
                rs = ext.mig_retrieve_nums_batch(rel_name::text, id_exp, ehr_id_exp, version_exp, batch_size);
                updated_cnt = COALESCE(cardinality(rs), 0);
            END;

            IF updated_cnt = 0 THEN
                RAISE NOTICE '[%] read 0. Finished pre-migration for %! in %s', rel_name, rel_name, to_char(clock_timestamp() - last, 'HH24:MI:SS:MS');
                EXIT;
            ELSEIF updated_cnt <> -1 THEN
                RAISE NOTICE '[%] read % in %', rel_name, updated_cnt, to_char(clock_timestamp() - last, 'HH24:MI:SS:MS');
            ELSE
                ROLLBACK;
                RAISE NOTICE '[%] error caught, waiting for 2s: (%) %', rel_name, err_state, err_msg;
                PERFORM pg_sleep(2);
                CONTINUE;
            END IF;

            last = clock_timestamp();

            BEGIN
                rs = ext.mig_calc_nums(rs);
                EXECUTE format('
                    UPDATE %1$s u
                    SET parent_num=r.parent_num, num_cap=r.num_cap
                    FROM unnest($1) r
                    WHERE u.vo_id=r.vo_id and u.num=r.num
                ', rel_name) USING rs;
                GET DIAGNOSTICS updated_cnt := ROW_COUNT;
            END;

            IF updated_cnt <> -1 THEN
                COMMIT;
                RAISE NOTICE '[%] updated % in %', rel_name, updated_cnt, to_char(clock_timestamp() - last, 'HH24:MI:SS:MS');
            ELSE
                ROLLBACK;
                RAISE NOTICE '[%] error caught, waiting for 2s: (%) %', rel_name, err_state,err_msg;
                PERFORM pg_sleep(2);
            END IF;

            last = clock_timestamp();
        END LOOP;
END;$procedure$;

--
-- Prepare Migrate
--

--Add new columns
ALTER TABLE ehr_folder_data_history
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE ehr_folder_data
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE ehr_status_data_history
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE ehr_status_data
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE comp_data_history
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;
ALTER TABLE comp_data
    ADD COLUMN IF NOT EXISTS parent_num integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_cap integer NOT NULL DEFAULT -1;

--
-- Migrate
--

--may be executed in parallel
CALL ext.mig_num_columns('ehr_folder_data_history'::regclass, 'ehr_id',100);
CALL ext.mig_num_columns('ehr_folder_data'::regclass, 'ehr_id',1000);
CALL ext.mig_num_columns('ehr_status_data_history'::regclass, 'ehr_id',10000);
CALL ext.mig_num_columns('ehr_status_data'::regclass, 'ehr_id',10000);
CALL ext.mig_num_columns('comp_data_history'::regclass, 'vo_id',1000);
CALL ext.mig_num_columns('comp_data'::regclass, 'vo_id'::text,1000);

-- Create new indices for path
CREATE INDEX IF NOT EXISTS ehr_status_data_path_idx ON ehr_status_data (ehr_id, parent_num, entity_attribute, entity_concept, rm_entity, num, num_cap);
CREATE INDEX IF NOT EXISTS comp_data_path_idx ON comp_data (vo_id, parent_num, entity_attribute, entity_concept, rm_entity, num, num_cap);

--
-- Post Migrate
--

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

-- Cleanup functions, procedures and types used for the migration
DROP FUNCTION IF EXISTS ext.mig_calc_nums(ext.mig_num_type[]);
DROP FUNCTION IF EXISTS ext.mig_retrieve_nums_batch(text, text, text, text, text, text, integer);
DROP PROCEDURE IF EXISTS ext.mig_num_columns(regclass, text, text, integer);
DROP TYPE IF EXISTS ext.mig_num_type CASCADE;
