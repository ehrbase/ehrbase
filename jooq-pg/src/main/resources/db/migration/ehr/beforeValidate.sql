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

-- This script is to update checksums or run additional migration if an older script was changed for some reason
DO
$$
    BEGIN
        IF (EXISTS(SELECT 1 WHERE to_regclass('${ehrSchema}.flyway_schema_history') is not null))
        THEN
            -- update changed checksums
            UPDATE ${ehrSchema}.flyway_schema_history fsh
            SET checksum = fsm.checksum
            FROM (SELECT version, checksum, UNNEST(old_checksums) old_checksum
                  from (VALUES ('1', 448539752, ARRAY [-1149994491, -1452840690, -121503791, -1072148350]),
                               ('2', -1029674754, ARRAY [1862075374, 1307015742]),
                               ('3', 395580994, ARRAY [-523819672, -2001859703, 1487580675, -2083264308]),
                               ('4', 679006617, ARRAY [172189345]),
                               ('5.1', 1877882752, ARRAY [-1135434127]),
                               ('5.2', -141620234, ARRAY [1084248966]),
                               ('5.3', -143024102, ARRAY [-1936979560]),
                               ('5.4', -162343166, ARRAY [2111260441]),
                               ('6.1', 1700747973, ARRAY [843702231]),
                               ('6.2', 774348872, ARRAY [-341300706]),
                               ('6.3', -324341521, ARRAY [-484834156]),
                               ('6.4', 2099076583, ARRAY [168068664, 1973447348, -1102016161]),
                               ('7', 614774845, ARRAY [-1037921140]),
                               ('8', -68902620, ARRAY [1018446818]),
                               ('9.1', -134441953, ARRAY [706120240]),
                               ('9.2', -1749209612, ARRAY [-772044287]),
                               ('9.3', 1631396860, ARRAY [-410614996]),
                               ('10', 284282285, ARRAY [906417882]),
                               ('11', 1312863182, ARRAY [1973315905]),
                               ('12', -1180956002, ARRAY [-1833794828]),
                               ('13', 2140263155, ARRAY [1159298511]),
                               ('14', -263968835, ARRAY [-1374259243]),
                               ('15', -1838484272, ARRAY [1525464025])
                        ) v(version, checksum, old_checksums)) fsm
            WHERE fsh.version = fsm.version
              AND fsh.checksum = fsm.old_checksum;
        END IF;
    END
$$;
