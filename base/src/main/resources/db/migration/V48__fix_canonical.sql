-- missing type...
CREATE OR REPLACE FUNCTION ehr.js_party_ref(TEXT, TEXT, TEXT, TEXT)
    RETURNS JSON AS
$$
DECLARE
    id_value ALIAS FOR $1;
    id_scheme ALIAS FOR $2;
    namespace ALIAS FOR $3;
    party_type ALIAS FOR $4;
BEGIN

    IF (id_value IS NULL AND id_scheme IS NULL AND namespace IS NULL AND party_type IS NULL) THEN
        RETURN NULL;
    ELSE
        RETURN
            json_build_object(
                    '_type', 'PARTY_REF',
                    'id',
                    json_build_object(
                            '_type', 'GENERIC_ID',
                            'value', id_value,
                            'scheme', id_scheme
                        ),
                    'namespace', namespace,
                    'type', party_type
                );
    END IF;
END
$$
    LANGUAGE plpgsql;


-- fix wrong encoding of code_phrase
CREATE OR REPLACE FUNCTION ehr.js_dv_coded_text(dvcodedtext ehr.dv_coded_text)
    RETURNS JSON AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', (SELECT (
                                             CASE
                                                 WHEN ((dvcodedtext).defining_code IS NOT NULL)
                                                     THEN
                                                     'DV_CODED_TEXT'
                                                 ELSE
                                                     'DV_TEXT'
                                                 END
                                             )
                ),
                        'value', dvcodedtext.value,
                        'defining_code', ehr.js_code_phrase(dvcodedtext.defining_code),
                        'formatting', dvcodedtext.formatting,
                        'language', dvcodedtext.language,
                        'encoding', dvcodedtext.encoding,
                        'mappings', ehr.js_term_mappings(dvcodedtext.term_mapping)
                    )
            );
END
$$
    LANGUAGE plpgsql;

-- make sure composition name is true DV_TEXT
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
                            'name', ehr.js_dv_text((entry_content.name).value),
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