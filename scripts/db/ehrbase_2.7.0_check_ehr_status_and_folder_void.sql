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

SET search_path = ehr;

--
-- Structure holding the root and history ehr_id as well as sys_version and void
--
DROP TYPE IF EXISTS pg_temp.mig_ehr_status_folder_data CASCADE;
CREATE TYPE pg_temp.mig_ehr_status_folder_data AS
(
    ehr_id uuid,
    root_version integer,
    root_void uuid,
    history_version integer,
    history_void uuid
);

--
-- Migration procedure
--
CREATE OR REPLACE PROCEDURE pg_temp.mig_ehr_status_folder_check(
    name text,
    batch_size integer DEFAULT 1000
) LANGUAGE plpgsql
AS $procedure$
DECLARE
    matches pg_temp.mig_ehr_status_folder_data[];
    matches_count integer := -1;
    batch_count integer := 0;
    match pg_temp.mig_ehr_status_folder_data;
    last timestamp;
BEGIN

    -- #1 Select Batch
    EXECUTE format('
        SELECT array_agg(history_matches)
            FROM(
               SELECT
                  root.ehr_id,
                  root.sys_version,
                  root.vo_id,
                  history.sys_version,
                  history.vo_id
               FROM %1$s as history
                  INNER JOIN %1$s root
                      ON root.ehr_id = history.ehr_id  -- of same EHR
                     AND root.sys_version = 1          -- take root version
                  JOIN %2$s as data
                     ON root.ehr_id = data.ehr_id      -- also join data
               WHERE root.vo_id != history.vo_id       -- where the root void does not match history void
                  OR root.vo_id != data.vo_id          -- or root void does not match actual data void
        ) history_matches
        LIMIT $1', name || '_data_history', name || '_data')
        INTO matches
        USING batch_size;

    matches_count = COALESCE(cardinality(matches), 0);

    IF matches_count = 0 THEN
        RAISE NOTICE '[%] batch % - matches(%) - no migration needed', name, batch_count, matches_count;
    ELSE -- updated_cnt <> -1 THEN
        RAISE NOTICE '[%] batch % - matches(%) (and possible more)', name, batch_count, matches_count;

        batch_count = batch_count + 1;
        last = clock_timestamp();

        -- #2 Log matches
        FOREACH match IN ARRAY matches LOOP
            RAISE NOTICE '[%] batch % - match{ehr_id: %, root_void(%): %, history_void(%): %}', name, batch_count, match.ehr_id, match.root_version, match.root_void, match.history_version, match.history_void;
        END LOOP;
    END IF;

END;$procedure$;

--
-- Migration Affected tables
--
CALL pg_temp.mig_ehr_status_folder_check('ehr_status', 100);
CALL pg_temp.mig_ehr_status_folder_check('ehr_folder', 100);



SELECT
    root.ehr_id,
    root.sys_version,
    root.vo_id,
    history.sys_version,
    history.vo_id
FROM ehr_status_data_history as history
     INNER JOIN ehr_status_data_history root
         ON root.ehr_id = history.ehr_id
        AND root.sys_version = 1          -- take root version
     JOIN ehr_status_data as data
        ON root.ehr_id = data.ehr_id
WHERE root.vo_id != history.vo_id
   OR root.vo_id != data.vo_id
LIMIT 1000;


SELECT
    root.ehr_id,
    root.sys_version,
    root.vo_id,
    history.sys_version,
    history.vo_id
FROM ehr_status_data_history as history
    INNER JOIN ehr_status_data_history root
         ON root.ehr_id = history.ehr_id  -- of same EHR
        AND root.sys_version = 1          -- take root version
    JOIN ehr_status_data as data
        ON root.ehr_id != data.ehr_id   -- also join data
WHERE root.vo_id != history.vo_id     -- where the root void does not match history void
   OR root.vo_id != data.vo_id        -- or root void does not match actual data void
LIMIT 1000;