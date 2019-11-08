-- convert a db dv_quantity into its canonical representation
-- DB representation:
-- {"units": "mg", "accuracy": 0.0, "magnitude": 636.3397240638733, "precision": 0, "accuracyPercent": false, "measurementService": {}}
-- Canonical comes out with type

CREATE OR REPLACE FUNCTION ehr.js_canonical_dv_quantity(magnitude FLOAT, units TEXT, _precision INT, accuracy_percent BOOLEAN)
  RETURNS JSON AS
$$
BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
            '_type', 'DV_QUANTITY',
            'magnitude', magnitude,
            'units', units,
            'precision', _precision,
            'accuracy_is_percent', accuracy_percent
          )
      );
END
$$
LANGUAGE plpgsql;

--fixed bad encoding
CREATE OR REPLACE FUNCTION ehr.js_dv_date_time(TIMESTAMP, TEXT)
  RETURNS JSON AS
$$
DECLARE
  date_time ALIAS FOR $1;
  time_zone ALIAS FOR $2;
BEGIN

  IF (date_time IS NULL)
  THEN
    RETURN NULL;
  END IF;

  IF (time_zone IS NULL)
  THEN
    time_zone := 'UTC';
  END IF;

  RETURN
    json_build_object(
        '_type', 'DV_DATE_TIME',
        'value', ehr.iso_timestamp(date_time)||time_zone
      );
END
$$
  LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.js_canonical_hier_object_id(ehr_id UUID)
  RETURNS JSON AS
$$
BEGIN
  RETURN
    json_build_object(
        '_type', 'HIER_OBJECT_ID',
        'value', ehr_id
      );
END
$$
  LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.js_canonical_generic_id(scheme TEXT, id TEXT)
  RETURNS json AS
$$
BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
                '_type', 'GENERIC_ID',
                'value', id,
                'scheme', scheme
              )
          );
END
$$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.js_canonical_party_ref(namespace TEXT, type TEXT, scheme TEXT, id TEXT)
  RETURNS json AS
$$
BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
            '_type', 'PARTY_REF',
            'namespace', namespace,
            'type', type,
            'id', ehr.js_canonical_generic_id(scheme, id)
          )
      );
END
$$
LANGUAGE plpgsql;


-- some minor fixes to support the 'new' canonical json format
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
  v_setting            UUID;
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

  json_context_query := ' SELECT json_build_object(
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

  json_context_query := json_context_query || '''setting'',ehr.js_context_setting(''' || v_setting || '''))';


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
LANGUAGE plpgsql;;

-- return the composition name as extracted from the jsonb entry
CREATE OR REPLACE FUNCTION ehr.composition_name(content JSONB)
  RETURNS TEXT AS
$$
BEGIN
  RETURN
    (with root_json as (
      select jsonb_object_keys(content) root)
     select trim(LEADING '''' FROM (trim(TRAILING ''']' FROM (regexp_split_to_array(root_json.root, 'and name/value='))[2])))
     from root_json
     where root like '/composition%');
END
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.composition_uid(composition_uid UUID, server_id TEXT)
  RETURNS TEXT AS
$$
BEGIN
  select "composition_join"."id"||'::'||server_id||'::'||1
        + COALESCE(
          (select count(*)
            from "ehr"."composition_history"
              where "composition_join"."id" = "ehr"."composition_history"."id"
              group by "ehr"."composition_history"."id")
          , 0) as "uid/value"
        from "ehr"."entry"
            right outer join "ehr"."composition" as "composition_join"
                        on "composition_join"."id" = composition_uid;
END
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_archetype_details(archetype_node_id TEXT, template_id TEXT)
  RETURNS jsonb AS
$$
BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
            '_type', 'ARCHETYPED',
            'archetype_id', jsonb_build_object (
                '_type', 'ARCHETYPE_ID',
                'value', archetype_node_id
            ),
            'template_id', jsonb_build_object (
                '_type', 'TEMPLATE_ID',
                'value', template_id
              ),
            'rm_version', '1.0.2'
          )
      );
END
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_object_version_id(version_id TEXT)
  RETURNS jsonb AS
$$
BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
            '_type', 'OBJECT_VERSION_ID',
            'value', version_id
          )
      );
END
$$
  LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.composition_uid(composition_uid UUID, server_id TEXT)
  RETURNS TEXT AS
$$
BEGIN
  RETURN (select "composition"."id"||'::'||server_id||'::'||1
    + COALESCE(
                                                                (select count(*)
                                                                 from "ehr"."composition_history"
                                                                 where "composition"."id" = composition_uid
                                                                 group by "ehr"."composition_history"."id")
                                                              , 0)
          from ehr.composition
          where composition.id = composition_uid);
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
        concept.conceptid     as category_defining_code,
        concept.description   as category_description,
        entry.entry           as content
      FROM ehr.composition
             INNER JOIN ehr.entry ON entry.composition_id = composition.id
             LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
             LEFT JOIN ehr.territory ON territory.code = composition.territory
             LEFT JOIN ehr.concept ON concept.id = entry.category
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
              'category',
              ehr.js_dv_coded_text(category_description, ehr.js_code_phrase(category_defining_code :: TEXT, 'openehr')),
              'context', ehr.js_context(context_id),
              'content', entry_content.jsonvalue::jsonb
            )
        )
    FROM entry_content
  );
END
$$
  LANGUAGE plpgsql;


