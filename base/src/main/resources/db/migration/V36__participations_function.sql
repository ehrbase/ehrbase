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

-- used to convert existing mode as a proper ehr.dv_coded_text type
CREATE OR REPLACE FUNCTION ehr.migrate_participation_function(mode TEXT)
    RETURNS ehr.dv_coded_text AS
$$
BEGIN
    RETURN (mode, NULL, NULL, NULL, NULL)::ehr.dv_coded_text;
END
$$
    LANGUAGE plpgsql;

ALTER TABLE ehr.participation
    ALTER COLUMN function TYPE ehr.dv_coded_text
        USING ehr.migrate_participation_function(function);

ALTER TABLE ehr.participation_history
    ALTER COLUMN function TYPE ehr.dv_coded_text
        USING ehr.migrate_participation_function(function);

-- returns an array of canonical participations
CREATE OR REPLACE FUNCTION ehr.js_participations(event_context_id UUID)
    RETURNS JSONB[] AS
$$
DECLARE
    item               JSONB;
    arr                JSONB[];
    participation_data RECORD;
BEGIN

    FOR participation_data IN
        SELECT participation.performer as performer,
               participation.function  as function,
               participation.mode      as mode,
               participation.time_lower,
               participation.time_lower_tz,
               participation.time_upper,
               participation.time_upper_tz
        FROM ehr.participation
        WHERE event_context = event_context_id
        LOOP
            item :=
                    jsonb_strip_nulls(
                            jsonb_build_object(
                                    '_type', 'PARTICIPATION',
                                    'function', (SELECT (
                                                            CASE
                                                                WHEN ((participation_data.function).defining_code IS NOT NULL)
                                                                    THEN
                                                                    ehr.js_dv_coded_text_inner(participation_data.function)
                                                                ELSE
                                                                    ehr.js_dv_text((participation_data.function).value)
                                                                END
                                                            )
                                    ),
                                    'performer', ehr.js_canonical_party_identified(participation_data.performer),
                                    'mode', ehr.js_dv_coded_text_inner(participation_data.mode),
                                    'time', (SELECT (
                                                        CASE
                                                            WHEN (participation_data.time_lower IS NOT NULL OR
                                                                  participation_data.time_upper IS NOT NULL) THEN
                                                                jsonb_build_object(
                                                                        '_type', 'DV_INTERVAL',
                                                                        'lower', ehr.js_dv_date_time(
                                                                                participation_data.time_lower,
                                                                                participation_data.time_lower_tz),
                                                                        'upper', ehr.js_dv_date_time(
                                                                                participation_data.time_upper,
                                                                                participation_data.time_upper_tz)
                                                                    )
                                                            ELSE
                                                                NULL
                                                            END
                                                        )
                                    )
                                )
                        );
            arr := array_append(arr, item);
        END LOOP;
    RETURN arr;
END
$$
    LANGUAGE plpgsql;