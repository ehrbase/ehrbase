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

-- This script is used to update checksums or run additional migration if an older script was changed for some reason
DO
$$
    BEGIN
        IF (EXISTS(SELECT 1 WHERE to_regclass('ext.flyway_schema_history') is not null))
        THEN
            -- update changed checksums
            UPDATE ext.flyway_schema_history fsh
            SET checksum = fsm.checksum
            FROM (SELECT version, checksum, UNNEST(old_checksums) old_checksum
                  from (VALUES ('1', -220483277, ARRAY [613591637]),
                               ('2', -1308335629, ARRAY [590834638]),
                               ('3', 1472329167, ARRAY [-624285813]),
                               ('4', 1058410942, ARRAY [-1953950631])) v(version, checksum, old_checksums)) fsm
            WHERE fsh.version = fsm.version
              AND fsh.checksum = fsm.old_checksum;
        END IF;
    END
$$;
