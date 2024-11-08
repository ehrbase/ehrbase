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

-- ALTER TABLE ehr_folder_data ADD PRIMARY KEY (vo_id, num);



ALTER TABLE ehr_folder_data ADD item_uuids uuid[] NULL DEFAULT NULL;
ALTER TABLE ehr_folder_data_history ADD item_uuids uuid[] NULL DEFAULT NULL;

-- 1.
DROP TYPE IF EXISTS mig_num_type CASCADE;

CREATE TYPE mig_num_type AS
(
    ehr_id uuid
    , num integer
    , ehr_folders_idx integer
    , item_uuids uuid[]
);

-- 2.
DROP FUNCTION IF EXISTS split_folder_items(regclass,integer);

CREATE OR REPLACE FUNCTION split_folder_items(
    rel_name regclass,
    batch_size integer
) RETURNS mig_num_type[]
    LANGUAGE plpgsql
    IMMUTABLE STRICT
AS $function$
DECLARE
    rs mig_num_type[];
    deb text;
BEGIN
    deb = format('
        SELECT array_agg(folder.r)
        FROM (
            SELECT
                row(
                    fd.ehr_id,
                    fd.num,
                    fd.ehr_folders_idx,
                    ARRAY_REMOVE(ARRAY_AGG((items -> %2$L ->> %3$L)::uuid), NULL)
                ) as r
            FROM %1$s fd
            LEFT JOIN JSONB_ARRAY_ELEMENTS(fd.data -> %4$L) as items ON value -> %2$L ->> %5$L = %6$L
            WHERE fd.item_uuids IS NULL
            GROUP BY fd.ehr_id, fd.ehr_folders_idx, fd.num
            LIMIT $1
        ) as folder
    ', rel_name::text, 'X', 'V', 'i', 'T', 'HX');
    
    EXECUTE deb
    INTO rs
    USING batch_size;
    
    return rs;
END;
$function$;

-- 3.
DROP PROCEDURE IF EXISTS mig_folder_items(regclass,integer);

CREATE OR REPLACE PROCEDURE mig_folder_items(
    rel_name regclass,
    batch_size integer
) LANGUAGE plpgsql
AS $procedure$
DECLARE
    ret mig_num_type[];
    mig_end integer;
    mig_start_time timestamp;
    mig_time timestamp;
    
--    deb text;
BEGIN
    RAISE NOTICE 'Start migration for %', rel_name;

    mig_start_time = clock_timestamp();

    mig_end = -1;
    
    WHILE mig_end  <> 0 LOOP
        mig_time = clock_timestamp();
    
        ret = split_folder_items(rel_name, batch_size);
        mig_end = COALESCE(cardinality(ret), 0);
        
        IF mig_end = 0 THEN
            RAISE NOTICE 'Finished pre-migration for % in %', rel_name, to_char(clock_timestamp() - mig_start_time, 'HH24:MI:SS:MS');
            EXIT;
        END IF;
    
        EXECUTE format('
            UPDATE %1$s fd
            SET item_uuids = a.item_uuids
            FROM UNNEST($1) a
            WHERE fd.ehr_id = a.ehr_id
            AND fd.ehr_folders_idx = a.ehr_folders_idx
            AND fd.num = a.num
        ', rel_name) USING ret;
    
        GET DIAGNOSTICS mig_end := ROW_COUNT;
        COMMIT;
        RAISE NOTICE '[%] updated % in %', rel_name, mig_end, to_char(clock_timestamp() - mig_time, 'HH24:MI:SS:MS');
    END LOOP;
END;
$procedure$;

-- 4.
CALL mig_folder_items('ehr_folder_data_history'::regclass, 100);
CALL mig_folder_items('ehr_folder_data'::regclass, 100);

DROP PROCEDURE IF EXISTS mig_folder_items(regclass,integer);
DROP FUNCTION IF EXISTS split_folder_items(regclass,integer);
DROP TYPE IF EXISTS mig_num_type CASCADE;




