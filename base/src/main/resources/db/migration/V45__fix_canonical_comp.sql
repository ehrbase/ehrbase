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
-- this fixes querying composition with no content
CREATE OR REPLACE FUNCTION ehr.js_composition(UUID, server_node_id TEXT)
    RETURNS JSON AS
$$
DECLARE
    composition_uuid ALIAS FOR $1;
BEGIN
    RETURN (
        WITH entry_content AS (
            SELECT
                composition.id as composition_id,
                composition.language  as language,
                composition.territory as territory,
                composition.composer  as composer,
                event_context.id      as context_id,
                territory.twoletter   as territory_code,
                entry.template_id     as template_id,
                entry.archetype_id    as archetype_id,
                entry.rm_version      as rm_version,
                entry.entry           as content,
                entry.category        as category,
                entry.name            as name,
                (SELECT jsonb_content FROM
                    (SELECT to_jsonb(jsonb_each(to_jsonb(jsonb_each((entry.entry)::jsonb)))) #>> '{value}' as jsonb_content) selcontent
                 WHERE jsonb_content::text like '{"%/content%' LIMIT 1) as json_content
            FROM ehr.composition
                     INNER JOIN ehr.entry ON entry.composition_id = composition.id
                     LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
                     LEFT JOIN ehr.territory ON territory.code = composition.territory
            WHERE composition.id = composition_uuid
        )
        SELECT
            jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'COMPOSITION',
                            'name', ehr.js_dv_coded_text(entry_content.name),
                            'archetype_details', ehr.js_archetype_details(entry_content.archetype_id, entry_content.template_id, entry_content.rm_version),
                            'archetype_node_id', entry_content.archetype_id,
                            'uid', ehr.js_object_version_id(ehr.composition_uid(entry_content.composition_id, server_node_id)),
                            'language', ehr.js_code_phrase(language, 'ISO_639-1'),
                            'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
                            'composer', ehr.js_canonical_party_identified(composer),
                            'category', ehr.js_dv_coded_text(category),
                            'context', ehr.js_context(context_id),
                            'content', entry_content.json_content::jsonb
                        )
                )
        FROM entry_content
    );
END
$$
    LANGUAGE plpgsql;