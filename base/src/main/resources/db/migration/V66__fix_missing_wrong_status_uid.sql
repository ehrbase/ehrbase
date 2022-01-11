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
-- added missing server_id
CREATE OR REPLACE FUNCTION ehr.js_ehr(UUID, TEXT)
    RETURNS JSON AS
$$
DECLARE
    ehr_uuid ALIAS FOR $1;
    server_id ALIAS FOR $2;
    contribution_json_array JSONB[];
    contribution_details JSONB;
    composition_version_json_array JSONB[];
    composition_in_ehr_id RECORD;
    folder_version_json_array JSONB[];
    folder_in_ehr_id RECORD;
BEGIN

    FOR contribution_details IN (SELECT ehr.js_contribution(contribution.id, server_id)
                                 FROM ehr.contribution
                                 WHERE contribution.ehr_id = ehr_uuid AND contribution.contribution_type != 'ehr')
        LOOP
            contribution_json_array := array_append(contribution_json_array, contribution_details);
        END LOOP;

    FOR composition_in_ehr_id IN (SELECT composition.id, composition.sys_transaction
                                  FROM ehr.composition
                                  WHERE composition.ehr_id = ehr_uuid)
        LOOP
            composition_version_json_array := array_append(
                    composition_version_json_array,
                    jsonb_build_object(
                            '_type', 'VERSIONED_COMPOSITION',
                            'id', ehr.js_object_version_id(ehr.composition_uid(composition_in_ehr_id.id, server_id)),
                            'time_created', ehr.js_dv_date_time(composition_in_ehr_id.sys_transaction, 'Z')
                        )
                );
        END LOOP;

    FOR folder_in_ehr_id IN (SELECT folder.id, folder.sys_transaction
                             FROM ehr.folder
                                      JOIN ehr.contribution ON folder.in_contribution = contribution.id
                             WHERE contribution.ehr_id = ehr_uuid)
        LOOP
            folder_version_json_array := array_append(
                    folder_version_json_array,
                    ehr.js_folder(folder_in_ehr_id.id, server_id)
                );
        END LOOP;

    RETURN (
        WITH ehr_data AS (
            SELECT
                ehr.id as ehr_id,
                ehr.date_created  as date_created,
                ehr.date_created_tzid as date_created_tz,
                ehr.access as access,
                system.settings as system_value,
                ehr.directory as directory
            FROM ehr.ehr
                     JOIN ehr.system ON system.id = ehr.system_id
            WHERE ehr.id = ehr_uuid
        )
        SELECT
            jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'EHR',
                            'ehr_id', ehr.js_canonical_hier_object_id(ehr_data.ehr_id),
                            'system_id', ehr.js_canonical_hier_object_id(ehr_data.system_value),
                            'ehr_status', ehr.js_ehr_status(ehr_data.ehr_id, server_id),
                            'time_created', ehr.js_dv_date_time(ehr_data.date_created, ehr_data.date_created_tz),
                            'contributions', contribution_json_array,
                            'compositions', composition_version_json_array,
                            'folders', folder_version_json_array,
                            'directory', ehr.js_folder(directory, server_id)
                        )
                -- 'ehr_access'
                -- 'tags'
                )

        FROM ehr_data
    );
END
$$
    LANGUAGE plpgsql;

-- use the status id (was ehr_id!)
CREATE OR REPLACE FUNCTION ehr.ehr_status_uid(ehr_uuid UUID, server_id TEXT)
    RETURNS TEXT AS
$$
BEGIN
    RETURN (select "status"."id"||'::'||server_id||'::'||1
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