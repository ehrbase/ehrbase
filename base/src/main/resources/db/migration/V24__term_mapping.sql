-- this migration implements term mapping in DvCodedText at DB level
-- NB. it deprecates using table CONCEPT as well

-- alter defined ehr.dv_coded_text
-- This representation is used as a clean typed definition fails at read time (jooq 3.12)
alter type ehr.dv_coded_text
  add attribute term_mapping TEXT[]; -- array : match, purpose: value, terminology, code, target: terminology, code, delimited by '|'

-- prepare the table migration
CREATE OR REPLACE FUNCTION ehr.migrate_category(category UUID)
  RETURNS ehr.dv_coded_text AS
$$
BEGIN
  RETURN (
    WITH concept_val AS (
      SELECT
        conceptid as code,
        description
      FROM ehr.concept
      WHERE concept.id = category
      LIMIT 1
    )
    select (concept_val.code, ('openehr', concept_val.description)::ehr.code_phrase, null, null, null, null)::ehr.dv_coded_text
    from concept_val
  );
END
$$
  LANGUAGE plpgsql;

-- AQL service functions
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
  json_context_query   TEXT;
  json_context         JSON;
  v_start_time         TIMESTAMP;
  v_start_time_tzid    TEXT;
  v_end_time           TIMESTAMP;
  v_end_time_tzid      TEXT;
  v_facility           UUID;
  v_location           TEXT;
  v_other_context      JSON;
  v_setting            TEXT;
BEGIN

  IF (context_id IS NULL)
  THEN
    RETURN NULL;
  END IF;

  -- build the query
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
        INTO v_start_time, v_start_time_tzid, v_end_time, v_end_time_tzid, v_facility, v_location, v_other_context, v_setting;

  json_context_query := ' SELECT jsonb_strip_nulls(jsonb_build_object(
                                  ''_time'', ''EVENT_CONTEXT'',
                                  ''start_time'', ehr.js_dv_date_time(''' || v_start_time || ''',''' ||
                        v_start_time_tzid || '''),';

  IF (v_end_time IS NOT NULL)
  THEN
    json_context_query :=
          json_context_query || '''end_date'', ehr.js_dv_date_time(''' || v_end_time || ''',''' || v_end_time_tzid ||
          '''),';
  END IF;

  IF (v_location IS NOT NULL)
  THEN
    json_context_query := json_context_query || '''location'', ''' || v_location || ''',';
  END IF;

  IF (v_facility IS NOT NULL)
  THEN
    json_context_query := json_context_query || '''health_care_facility'', ehr.js_party('''||v_facility||'''),';
  END IF;

  json_context_query := json_context_query || '''setting'',ehr.js_dv_coded_text(''' || v_setting || ''')))';


  --     IF (participation IS NOT NULL) THEN
  --       json_context_query := json_context_query || '''participation'', participation,';
  --     END IF;

  IF (json_context_query IS NULL)
  THEN
    RETURN NULL;
  END IF;

  EXECUTE json_context_query
    INTO STRICT json_context;

  IF (v_other_context IS NOT NULL)
  THEN
    json_context := jsonb_insert(
        json_context::JSONB,
        '{other_context}', v_other_context::JSONB
      )::JSON;
  END IF;

  RETURN json_context;
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
        entry.category        as category,
        entry.entry           as content
      FROM ehr.composition
             INNER JOIN ehr.entry ON entry.composition_id = composition.id
             LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
             LEFT JOIN ehr.territory ON territory.code = composition.territory
      WHERE composition.id = composition_uuid
      LIMIT 1
    ),
         entry_content AS (
           WITH values as (
             select composition_data.*,
                    to_jsonb(jsonb_each(to_jsonb(jsonb_each((composition_data.content)::jsonb)))) #>> '{value}' as jsonvalue
             from composition_data
             where composition_data.composition_id = composition_uuid
           )
           select values.*
           FROM values
           where jsonvalue like '{"/content%'
           LIMIT 1
         )
    SELECT
      jsonb_strip_nulls(
          jsonb_build_object(
              '_type', 'COMPOSITION',
              'name', ehr.js_dv_text(ehr.composition_name(entry_content.content)),
              'archetype_details', ehr.js_archetype_details(entry_content.archetype_id, entry_content.template_id),
              'archetype_node_id', entry_content.archetype_id,
              'uid', ehr.js_object_version_id(ehr.composition_uid(entry_content.composition_id, server_node_id)),
              'language', ehr.js_code_phrase(language, 'ISO_639-1'),
              'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
              'composer', ehr.js_party(composer),
              'category', ehr.js_dv_coded_text(category),
              'context', ehr.js_context(context_id),
              'content', entry_content.jsonvalue::jsonb
            )
        )
    FROM entry_content
  );
END
$$
  LANGUAGE plpgsql;

-- alter table entry & entry_history to use the new type
alter table ehr.entry drop constraint entry_category_fkey;

alter table ehr.entry
  alter column category type ehr.dv_coded_text
    using ehr.migrate_category(category);

alter table ehr.entry_history
  alter column category type ehr.dv_coded_text
    using ehr.migrate_category(category);

-- do the same with table event_context & event_context_history
alter table ehr.event_context drop constraint event_context_setting_fkey;

alter table ehr.event_context
  alter column setting type ehr.dv_coded_text
    using ehr.migrate_category(setting);

alter table ehr.event_context_history
  alter column setting type ehr.dv_coded_text
    using ehr.migrate_category(setting);

