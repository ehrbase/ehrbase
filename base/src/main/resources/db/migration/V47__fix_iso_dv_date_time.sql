/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

-- do not use the error prone XML date/time conversion
DROP FUNCTION ehr.js_dv_date_time(TIMESTAMPTZ, TEXT);
DROP FUNCTION ehr.js_dv_date_time(TIMESTAMP,text);

-- this is to fix the timezone drift and provide the correct encoding
CREATE OR REPLACE FUNCTION ehr.js_dv_date_time(TIMESTAMP, TEXT)
    RETURNS JSON AS
$$
DECLARE
    date_time ALIAS FOR $1;
    time_zone ALIAS FOR $2;
    value_date_time TEXT;
BEGIN

    IF (date_time IS NULL)
    THEN
        RETURN NULL;
    END IF;

    IF (time_zone IS NULL)
    THEN
        time_zone := 'Z';
    END IF;

    RETURN
        json_build_object(
                '_type', 'DV_DATE_TIME',
                'value',to_char(date_time, 'YYYY-MM-DD"T"HH24:MI:SS.MS"'||time_zone||'"')
            );
END
$$
    LANGUAGE plpgsql;