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

ALTER TABLE ehr_folder_data ADD IF NOT EXISTS item_uuids uuid[] NULL DEFAULT NULL;
ALTER TABLE ehr_folder_data_history ADD IF NOT EXISTS item_uuids uuid[] NULL DEFAULT NULL;

CREATE INDEX IF NOT EXISTS mig_ehr_folder_data_item_uuids ON ehr_folder_data (ehr_id) WHERE item_uuids IS NULL;
CREATE INDEX IF NOT EXISTS mig_ehr_folder_data_history_item_uuids ON ehr_folder_data_history (ehr_id) WHERE item_uuids IS NULL;

CREATE OR REPLACE PROCEDURE pg_temp.mig_folder_items(
    rel_name regclass,
    batch_size integer
) LANGUAGE plpgsql
AS $procedure$
DECLARE
    update_count integer := -1;
    mig_start_time timestamp := clock_timestamp();
    mig_time timestamp;
BEGIN
    RAISE NOTICE 'Start migration for % at %', rel_name, to_char(mig_start_time, 'HH24:MI:SS:MS');
    
    WHILE update_count  <> 0 LOOP
        mig_time = clock_timestamp();

        EXECUTE format($$
        UPDATE %1$I
        SET item_uuids = array(select (jsonb_array_elements(data -> 'i') -> 'x' ->> 'V')::uuid) --,
        --data = data - 'i' -- XXX omitting removing items from json for now
        WHERE item_uuids is null and ehr_id IN (SELECT ehr_id FROM %1$I WHERE item_uuids is null LIMIT $1);
        $$, rel_name::text)
        USING batch_size;

        GET DIAGNOSTICS update_count := ROW_COUNT;

        IF update_count = 0 THEN
            RAISE NOTICE 'Finished migration for % in %', rel_name, to_char(clock_timestamp() - mig_start_time, 'HH24:MI:SS:MS');
        ELSE
            COMMIT;
            RAISE NOTICE '[%] updated % in %', rel_name, update_count, to_char(clock_timestamp() - mig_time, 'HH24:MI:SS:MS');
        END IF;
    END LOOP;
END;
$procedure$;

CALL pg_temp.mig_folder_items('ehr_folder_data'::regclass, 10000);
CALL pg_temp.mig_folder_items('ehr_folder_data_history'::regclass, 1000);

ALTER TABLE ehr_folder_data_history ALTER item_uuids DROP DEFAULT, ALTER item_uuids SET NOT NULL;
ALTER TABLE ehr_folder_data ALTER item_uuids DROP DEFAULT, ALTER item_uuids SET NOT NULL;

DROP INDEX mig_ehr_folder_data_item_uuids;
DROP INDEX mig_ehr_folder_data_history_item_uuids;
