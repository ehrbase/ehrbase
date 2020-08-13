DROP FUNCTION ehr.js_composition(uuid,text);

CREATE OR REPLACE FUNCTION ehr.js_composition(UUID, server_node_id TEXT)
    RETURNS JSON AS
$$
DECLARE
    composition_uuid ALIAS FOR $1;
BEGIN
    RETURN (
        WITH composition_data AS (
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
                concept.conceptid     as category_defining_code,
                concept.description   as category_description,
                entry.entry           as content,
                to_jsonb(jsonb_each(to_jsonb(jsonb_each((entry.entry)::jsonb)))) #>> '{value}' as json_content
            FROM ehr.composition
                     INNER JOIN ehr.entry ON entry.composition_id = composition.id
                     LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
                     LEFT JOIN ehr.territory ON territory.code = composition.territory
                     LEFT JOIN ehr.concept ON concept.id = entry.category
            WHERE composition.id = composition_uuid
        ),
             entry_content AS (
                 SELECT * FROM composition_data
                 WHERE json_content::text like '{"/content%' OR json_content = '{}'
             )
        SELECT
            jsonb_strip_nulls(
                    jsonb_build_object(
                            '_type', 'COMPOSITION',
                            'name', ehr.js_dv_text(ehr.composition_name(entry_content.content)),
                            'archetype_details', ehr.js_archetype_details(entry_content.archetype_id, entry_content.template_id, entry_content.rm_version),
                            'archetype_node_id', entry_content.archetype_id,
                            'uid', ehr.js_object_version_id(ehr.composition_uid(entry_content.composition_id, server_node_id)),
                            'language', ehr.js_code_phrase(language, 'ISO_639-1'),
                            'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
                            'composer', ehr.js_canonical_party_identified(composer),
                            'category',
                            ehr.js_dv_coded_text(category_description, ehr.js_code_phrase(category_defining_code :: TEXT, 'openehr')),
                            'context', ehr.js_context(context_id),
                            'content', entry_content.json_content::jsonb
                        )
                )
        FROM entry_content
    );
END
$$
    LANGUAGE plpgsql;