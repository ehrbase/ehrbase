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

-- V35
-- this migration implements term mapping in DvCodedText at DB level

-- alter defined ehr.dv_coded_text
-- This representation is used as a clean typed definition fails at read time (jooq 3.12)
alter type ehr.dv_coded_text
    add attribute term_mapping TEXT[]; -- array : match, purpose: value, terminology, code, target: terminology, code, delimited by '|'


-- prepare the table migration
CREATE OR REPLACE FUNCTION ehr.migrate_concept_to_dv_coded_text(concept_id UUID)
    RETURNS ehr.dv_coded_text AS
$$
BEGIN
    RETURN (
        WITH concept_val AS (
            SELECT
                conceptid as code,
                description
            FROM ehr.concept
            WHERE concept.id = concept_id
            LIMIT 1
        )
        select (concept_val.code, ('openehr', concept_val.description)::ehr.code_phrase, null, null, null, null)::ehr.dv_coded_text
        from concept_val
    );
END
$$
    LANGUAGE plpgsql;

-- setting as DvCodedText
alter table ehr.event_context drop constraint event_context_setting_fkey;

alter table ehr.event_context
    alter column setting type ehr.dv_coded_text
        using ehr.migrate_concept_to_dv_coded_text(setting);

alter table ehr.event_context_history
    alter column setting type ehr.dv_coded_text
        using ehr.migrate_concept_to_dv_coded_text(setting);

alter table ehr.entry drop constraint entry_category_fkey;

alter table ehr.entry
    alter column category type ehr.dv_coded_text
        using ehr.migrate_concept_to_dv_coded_text(category);

alter table ehr.entry_history
    alter column category type ehr.dv_coded_text
        using ehr.migrate_concept_to_dv_coded_text(category);

-- AQL service functions
CREATE OR REPLACE FUNCTION ehr.js_dv_coded_text_inner(value TEXT, terminology_id TEXT, code_string TEXT)
    RETURNS JSON AS
$$
BEGIN
    RETURN
        json_build_object(
                '_type', 'DV_CODED_TEXT',
                'value', value,
                'defining_code', ehr.js_code_phrase(code_string, terminology_id)
            );
END
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_term_mappings(mappings TEXT[])
    RETURNS JSONB[] AS
$$
DECLARE
    encoded TEXT;
    attributes TEXT[];
    item JSONB;
    arr JSONB[];
BEGIN

    IF (mappings IS NULL) THEN
        RETURN NULL;
    end if;

    FOREACH encoded IN ARRAY mappings
        LOOP
        -- 	  RAISE NOTICE 'encoded %',encoded;
        -- the encoding is required since ARRAY in PG only support base types (e.g. no UDTs)
            attributes := regexp_split_to_array(encoded, '\|');
            item := jsonb_build_object(
                    '_type', 'TERM_MAPPING',
                    'match', attributes[1],
                    'purpose', ehr.js_dv_coded_text_inner(attributes[2], attributes[3], attributes[4]),
                    'target', ehr.js_code_phrase(attributes[6], attributes[5])
                );
            arr := array_append(arr, item);
        END LOOP;
    RETURN arr;
END
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_dv_coded_text(dvcodedtext ehr.dv_coded_text)
    RETURNS JSON AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', 'DV_CODED_TEXT',
                        'value', dvcodedtext.value,
                        'defining_code', dvcodedtext.defining_code,
                        'formatting', dvcodedtext.formatting,
                        'language', dvcodedtext.language,
                        'encoding', dvcodedtext.encoding,
                        'mappings', ehr.js_term_mappings(dvcodedtext.term_mapping)
                    )
            );
END
$$
    LANGUAGE plpgsql;

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
                                   'health_care_facility', ehr.js_party(facility),
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
                entry.entry           as content,
                entry.category        as category,
                to_jsonb(jsonb_each(to_jsonb(jsonb_each((entry.entry)::jsonb)))) #>> '{value}' as json_content
            FROM ehr.composition
                     INNER JOIN ehr.entry ON entry.composition_id = composition.id
                     LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
                     LEFT JOIN ehr.territory ON territory.code = composition.territory
            WHERE composition.id = composition_uuid
        ),
             entry_content AS (
                 SELECT * FROM composition_data
                 WHERE json_content::text like '{"%/content%' OR json_content = '{}'
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