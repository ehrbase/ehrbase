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
                            'subject', ehr.js_canonical_party_identified(subject),
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