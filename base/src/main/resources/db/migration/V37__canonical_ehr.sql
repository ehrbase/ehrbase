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

 -- use this mapping until audit details change_type is a dv_coded_text
 CREATE OR REPLACE FUNCTION ehr.map_change_type_to_codestring(literal TEXT)
     RETURNS TEXT AS
 $$
 BEGIN
     RETURN (
         CASE
             WHEN literal = 'creation' THEN '249'
             WHEN literal = 'amendment' THEN '250'
             WHEN literal = 'modification' THEN '251'
             WHEN literal = 'synthesis' THEN '252'
             WHEN literal = 'deleted' THEN '523'
             WHEN literal = 'attestation' THEN '666'
             WHEN literal = 'unknown' THEN '253'
             ELSE
                 '253'
             END
         );
 END
 $$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.js_audit_details(UUID)
    RETURNS JSON AS
$$
DECLARE
    audit_details_uuid ALIAS FOR $1;
BEGIN
    RETURN(
        SELECT
            jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'AUDIT_DETAILS',
                            'system_id', ehr.js_canonical_hier_object_id(system.settings),
                            'time_committed', ehr.js_dv_date_time(audit_details.time_committed, audit_details.time_committed_tzid),
                            'change_type', ehr.js_dv_coded_text_inner((audit_details.change_type,
                                                                       (('openehr', ehr.map_change_type_to_codestring(audit_details.change_type::TEXT))::ehr.code_phrase),
                                                                       NULL,
                                                                       NULL,
                                                                       NULL,
                                                                       NULL)::ehr.dv_coded_text),
                            'description', ehr.js_dv_text(audit_details.description),
                            'committer', ehr.js_canonical_party_identified(audit_details.committer)
                        )
                )
        FROM ehr.audit_details
                 JOIN ehr.system ON system.id = audit_details.system_id
        WHERE audit_details.id = audit_details_uuid
    );
END
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.folder_uid(folder_uid UUID, server_id TEXT)
    RETURNS TEXT AS
$$
BEGIN
    RETURN (
        select "folder_join"."id" || '::' || server_id || '::' || 1
            + COALESCE(
                  (select count(*)
                   from "ehr"."folder_history"
                   where folder_uid = "ehr"."folder_history"."id"
                   group by "ehr"."folder_history"."id")
              , 0) as "uid/value"
        from "ehr"."entry"
                 right outer join "ehr"."folder" as "folder_join"
                                  on "folder_join"."id" = folder_uid
        limit 1
    );
END
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_contribution(UUID, TEXT)
    RETURNS JSON AS
$$
DECLARE
    contribution_uuid ALIAS FOR $1;
    server_id ALIAS FOR $2;
BEGIN
    RETURN(
        SELECT
            jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'CONTRIBUTION',
                            'uid', ehr.js_canonical_hier_object_id(contribution.id),
                            'audit', ehr.js_audit_details(contribution.has_audit)
                        )
                )
        FROM ehr.contribution
        WHERE contribution.id = contribution_uuid
    );
END
$$
    LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.js_folder(folder_uid UUID, server_id TEXT)
RETURNS JSONB AS
$$
BEGIN

    IF (NOT EXISTS(SELECT * FROM ehr.folder WHERE id = folder_uid)) THEN
        RETURN NULL;
    end if;

    RETURN (
        WITH folder_data AS (
            SELECT name, sys_transaction
            FROM ehr.folder
            WHERE id = folder_uid
        )
        SELECT
             jsonb_build_object(
                 '_type', 'VERSIONED_FOLDER',
                 'id', ehr.js_object_version_id(ehr.folder_uid(folder_uid, server_id)),
                 'name', ehr.js_dv_text(folder_data.name),
                 'time_created', ehr.js_dv_date_time(folder_data.sys_transaction, 'Z')
        )
        FROM folder_data
    );
END
$$
LANGUAGE plpgsql;

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
                            'ehr_status', ehr.js_ehr_status(ehr_data.ehr_id),
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