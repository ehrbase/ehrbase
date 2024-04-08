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

-- This file may be used to update hash values or run additional migration if an older script was changed for some reason
DO
$$
    BEGIN
        IF (EXISTS
            (SELECT 1
             FROM information_schema.tables
             WHERE table_schema = '${ehrSchema}'
               AND table_name = 'flyway_schema_history'))
        THEN

            UPDATE ${ehrSchema}.flyway_schema_history
            SET checksum = -121503791
            WHERE version = '1'
              AND checksum IN (-1149994491, -1452840690);

            UPDATE ${ehrSchema}.flyway_schema_history
            SET checksum = 1307015742
            WHERE (version, checksum) = ('2', 1862075374);

            UPDATE ${ehrSchema}.flyway_schema_history
            SET checksum = 1487580675
            WHERE version = '3'
              AND checksum IN (-523819672, -2001859703);

            UPDATE ${ehrSchema}.flyway_schema_history
            SET checksum = 1973447348
            WHERE (version, checksum) = ('6.4', 168068664);

        END IF;
    END
$$;
