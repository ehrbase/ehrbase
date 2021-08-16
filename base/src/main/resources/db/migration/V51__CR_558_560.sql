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
-- CR 560. Add missing uid attribute
DROP FUNCTION  IF EXISTS ehr_status_uid(uuid,text);

CREATE OR REPLACE FUNCTION ehr.ehr_status_uid(ehr_uuid UUID, server_id TEXT)
    RETURNS TEXT AS
$$
BEGIN
    RETURN (select "status"."ehr_id"||'::'||server_id||'::'||1
        + COALESCE(
                 (select count(*)
                  from "ehr"."status_history"
                  where "status_history"."ehr_id" = ehr_uuid
                  group by "ehr"."status_history"."ehr_id")
             , 0)
            from ehr.status
            where status.ehr_id = ehr_uuid);
END
$$
    LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS js_ehr_status_uid(uuid,text);

CREATE OR REPLACE FUNCTION ehr.js_ehr_status_uid(ehr_uuid UUID, server_id TEXT)
    RETURNS JSONB AS
$$
BEGIN
    RETURN jsonb_strip_nulls(
            jsonb_build_object(
                    '_type', 'HIER_OBJECT_ID',
                    'value', ehr.ehr_status_uid(ehr_uuid, server_id)
                )
        );
END
$$
    LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS js_ehr_status(uuid,text);

CREATE OR REPLACE FUNCTION ehr.js_ehr_status(ehr_uuid UUID, server_id TEXT)
    RETURNS JSON AS
$$
BEGIN
    RETURN (
        WITH ehr_status_data AS (
            SELECT
                status.other_details as other_details,
                status.party as subject,
                status.is_queryable as is_queryable,
                status.is_modifiable as is_modifiable,
                status.sys_transaction as time_created,
                status.name as status_name,
                status.archetype_node_id as archetype_node_id
            FROM ehr.status
            WHERE status.ehr_id = ehr_uuid
            LIMIT 1
        )
        SELECT
            jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'EHR_STATUS',
                            'archetype_node_id', archetype_node_id,
                            'name', status_name,
                            'subject', ehr.js_party(subject),
                            'uid', ehr.js_ehr_status_uid(ehr_uuid, server_id),
                                'is_queryable', is_queryable,
                            'is_modifiable', is_modifiable,
                            'other_details', other_details
                        )
                )
        FROM ehr_status_data
    );
END
$$
    LANGUAGE plpgsql;

-- CR 558. Add missing attributes in DV_IDENTIFIER structure

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
            item := jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'DV_IDENTIFIER',
                            'id',identifier_attribute.id_value,
                            'assigner', identifier_attribute.assigner,
                            'issuer', identifier_attribute.issuer,
                            'type', identifier_attribute.type_name
                        )
                );
            arr := array_append(arr, item);
        END LOOP;

    SELECT
        jsonb_strip_nulls(
                jsonb_build_object (
                        '_type', 'PARTY_IDENTIFIED',
                        'name', name,
                        'identifiers', arr,
                        'external_ref', jsonb_build_object(
                                '_type', 'PARTY_REF',
                                'namespace', namespace,
                                'type', ref_type,
                                'id', ehr.js_canonical_object_id(objectid_type, scheme, id_value)
                            )
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
            item := jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'DV_IDENTIFIER',
                            'id',identifier_attribute.id_value,
                            'assigner', identifier_attribute.assigner,
                            'issuer', identifier_attribute.issuer,
                            'type', identifier_attribute.type_name
                        )
                );
            arr := array_append(arr, item);
        END LOOP;

    SELECT
        jsonb_strip_nulls(
                jsonb_build_object (
                        '_type', 'PARTY_RELATED',
                        'name', name,
                        'identifiers', arr,
                        'external_ref', jsonb_build_object(
                                '_type', 'PARTY_REF',
                                'namespace', namespace,
                                'type', ref_type,
                                'id', ehr.js_canonical_object_id(objectid_type, scheme, id_value)
                            ),
                        'relationship', ehr.js_dv_coded_text(relationship)
                    )
            )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$
    language 'plpgsql';
