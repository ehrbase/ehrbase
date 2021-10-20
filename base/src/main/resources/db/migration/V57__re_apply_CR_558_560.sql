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
                    'id',identifier_attribute.id_value,
                    'assigner', identifier_attribute.assigner,
                    'issuer', identifier_attribute.issuer,
                    'type', identifier_attribute.type_name
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
                    'assigner', identifier_attribute.assigner,
                    'issuer', identifier_attribute.issuer,
                    'type', identifier_attribute.type_name
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
