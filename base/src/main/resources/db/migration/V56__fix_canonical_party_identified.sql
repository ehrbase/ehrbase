/*
 *  Copyright (c) 2021 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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

-- invoke correct canonical encoder for health_care_facility
CREATE OR REPLACE FUNCTION ehr.js_context(UUID)
    RETURNS JSON AS
$$
DECLARE
    context_id ALIAS FOR $1;
BEGIN

    IF (context_id IS NULL)
    THEN
        RETURN NULL;
    ELSE
        RETURN (
            WITH context_attributes AS (
                SELECT
                    start_time,
                    start_time_tzid,
                    end_time,
                    end_time_tzid,
                    facility,
                    location,
                    other_context,
                    setting
                FROM ehr.event_context
                WHERE id = context_id
            )
            SELECT jsonb_strip_nulls(
                           jsonb_build_object(
                                   '_type', 'EVENT_CONTEXT',
                                   'start_time', ehr.js_dv_date_time(start_time, start_time_tzid),
                                   'end_time', ehr.js_dv_date_time(end_time, end_time_tzid),
                                   'location', location,
                                   'health_care_facility', ehr.js_canonical_party_identified(facility),
                                   'setting', ehr.js_dv_coded_text(setting),
                                   'other_context',other_context,
                                   'participations', ehr.js_participations(context_id)
                               )
                       )
            FROM context_attributes
        );
    END IF;
END
$$
    LANGUAGE plpgsql;

-- fix NULL external_ref representation
CREATE OR REPLACE FUNCTION ehr.party_ref(namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT, objectid_type ehr.party_ref_id_type)
    RETURNS jsonb AS
$$
BEGIN
    RETURN
        (SELECT (
                    CASE
                        WHEN (namespace IS NOT NULL AND ref_type IS NOT NULL) THEN
                            jsonb_build_object(
                                    '_type', 'PARTY_REF',
                                    'namespace', namespace,
                                    'type', ref_type,
                                    'id',
                                    ehr.js_canonical_object_id(objectid_type, scheme, id_value)
                                )
                        ELSE NULL
                        END
                    )
        );
END;
$$
    language 'plpgsql';

CREATE OR REPLACE FUNCTION ehr.json_party_self(refid UUID, namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT, objectid_type ehr.party_ref_id_type)
    RETURNS json AS
$$
DECLARE
    json_party_struct JSON;
BEGIN
    SELECT
        jsonb_strip_nulls(
                jsonb_build_object (
                        '_type', 'PARTY_SELF',
                        'external_ref', ehr.party_ref(namespace, ref_type, scheme, id_value, objectid_type)
                    )
            )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$
    language 'plpgsql';


CREATE OR REPLACE FUNCTION ehr.json_party_identified(name TEXT, refid UUID, namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT, objectid_type ehr.party_ref_id_type)
    RETURNS json AS
$$
DECLARE
    json_party_struct JSON;
    item JSONB;
    arr JSONB[];
    identifier_attribute record;
BEGIN
    -- build an array of json object from identifiers if any
    FOR identifier_attribute IN SELECT * FROM ehr.identifier WHERE identifier.party = refid LOOP
            item := jsonb_build_object(
                    '_type', 'DV_IDENTIFIER',
                    'id',identifier_attribute.id_value
                );
            arr := array_append(arr, item);
        END LOOP;

    SELECT
        jsonb_strip_nulls(
                jsonb_build_object (
                        '_type', 'PARTY_IDENTIFIED',
                        'name', name,
                        'identifiers', arr,
                        'external_ref', ehr.party_ref(namespace, ref_type, scheme, id_value, objectid_type)
                    )
            )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$
    language 'plpgsql';

CREATE OR REPLACE FUNCTION ehr.json_party_related(name TEXT, refid UUID, namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT, objectid_type ehr.party_ref_id_type, relationship ehr.dv_coded_text)
    RETURNS json AS
$$
DECLARE
    json_party_struct JSON;
    item JSONB;
    arr JSONB[];
    identifier_attribute record;
BEGIN
    -- build an array of json object from identifiers if any
    FOR identifier_attribute IN SELECT * FROM ehr.identifier WHERE identifier.party = refid LOOP
            item := jsonb_build_object(
                    '_type', 'DV_IDENTIFIER',
                    'id',identifier_attribute.id_value
                );
            arr := array_append(arr, item);
        END LOOP;

    SELECT
        jsonb_strip_nulls(
                jsonb_build_object (
                        '_type', 'PARTY_RELATED',
                        'name', name,
                        'identifiers', arr,
                        'external_ref', ehr.party_ref(namespace, ref_type, scheme, id_value, objectid_type),
                        'relationship', ehr.js_dv_coded_text(relationship)
                    )
            )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$
    language 'plpgsql';
