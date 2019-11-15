/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- archetyped.sql
CREATE OR REPLACE FUNCTION ehr.js_archetyped(TEXT, TEXT)
  RETURNS JSON AS
  $$
  DECLARE
    archetype_id ALIAS FOR $1;
    template_id ALIAS FOR $2;
  BEGIN
    RETURN
      json_build_object(
          '_type', 'ARCHETYPED',
          'archetype_id',
          json_build_object(
              '_type', 'ARCHETYPE_ID',
              'value', archetype_id
          ),
          template_id,
          json_build_object(
              '_type', 'TEMPLATE_ID',
              'value', template_id
          ),
          'rm_version', '1.0.1'
      );
  END
  $$
LANGUAGE plpgsql;

--code_phrase.sql
CREATE OR REPLACE FUNCTION ehr.js_code_phrase(TEXT, TEXT)
  RETURNS JSON AS
$$
DECLARE
  code_string ALIAS FOR $1;
  terminology ALIAS FOR $2;
BEGIN
  RETURN
  json_build_object(
      '_type', 'CODE_PHRASE',
      'terminology_id',
      json_build_object(
          '_type', 'TERMINOLOGY_ID',
          'value', terminology
      ),
      'code_string', code_string
  );
END
$$
LANGUAGE plpgsql;

--context.sql
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
    v_other_context      JSONB;
    v_other_context_text TEXT;
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
                                  ''_type'', ''EVENT_CONTEXT'',
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
      --       v_other_context_text := regexp_replace(v_other_context::TEXT, '''', '''''', 'g');
      json_context := jsonb_insert(
          json_context::JSONB,
          '{other_context}', v_other_context::JSONB->'/context/other_context[at0001]'
      );
    END IF;

    RETURN json_context;
  END
  $$
LANGUAGE plpgsql;

-- context_setting.sql
CREATE OR REPLACE FUNCTION ehr.js_context_setting(UUID)
  RETURNS JSON AS
  $$
  DECLARE
    concept_id ALIAS FOR $1;
  BEGIN

    IF (concept_id IS NULL) THEN
      RETURN NULL;
    END IF;

    RETURN (
      SELECT ehr.js_dv_coded_text(description, ehr.js_code_phrase(conceptid :: TEXT, 'openehr'))
      FROM ehr.concept
      WHERE id = concept_id AND language = 'en'
    );
  END
  $$
LANGUAGE plpgsql;

-- dv_coded_text.sql
CREATE OR REPLACE FUNCTION ehr.js_dv_coded_text(TEXT, JSON)
  RETURNS JSON AS
$$
DECLARE
  value_string ALIAS FOR $1;
  code_phrase ALIAS FOR $2;
BEGIN
  RETURN
  json_build_object(
      '_type', 'DV_CODED_TEXT',
      'value', value_string,
      'defining_code', code_phrase
  );
END
$$
LANGUAGE plpgsql;

-- dv_date_time.sql
CREATE OR REPLACE FUNCTION ehr.js_dv_date_time(TIMESTAMPTZ, TEXT)
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

-- dv_text.sql
CREATE OR REPLACE FUNCTION ehr.js_dv_text(TEXT)
  RETURNS JSON AS
$$
DECLARE
  value_string ALIAS FOR $1;
BEGIN
  RETURN
  json_build_object(
      '_type', 'DV_TEXT',
      'value', value_string
  );
END
$$
LANGUAGE plpgsql;

-- iso_timestamp.sql
create or replace function ehr.iso_timestamp(timestamp with time zone)
  returns varchar as $$
select substring(xmlelement(name x, $1)::varchar from 4 for 19)
$$ language sql immutable;

-- json_composition_pg10.sql
-- CTE enforces 1-to-1 entry-composition relationship since multiple entries can be
-- associated to one composition. This is not supported at this stage.
CREATE OR REPLACE FUNCTION ehr.js_composition(UUID)
  RETURNS JSON AS
  $$
  DECLARE
    composition_uuid ALIAS FOR $1;
  BEGIN
    RETURN (
      WITH composition_data AS (
          SELECT
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
      )
      SELECT
        jsonb_strip_nulls(
            jsonb_build_object(
                '_type', 'COMPOSITION',
                'language', ehr.js_code_phrase(language, 'ISO_639-1'),
                'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
                'composer', ehr.js_party(composer),
                'category',
                ehr.js_dv_coded_text(category_description, ehr.js_code_phrase(category_defining_code :: TEXT, 'openehr')),
                'context', ehr.js_context(context_id),
                'content', content
            )
        )
      FROM composition_data
    );
  END
  $$
LANGUAGE plpgsql;
-- object_version_id.sql
CREATE OR REPLACE FUNCTION ehr.object_version_id(UUID, TEXT, INT)
  RETURNS JSON AS
$$
DECLARE
  object_uuid ALIAS FOR $1;
  object_host ALIAS FOR $2;
  object_version ALIAS FOR $3;
BEGIN
  RETURN
  json_build_object(
      '_type', 'OBJECT_VERSION_ID',
      'value', object_uuid::TEXT || '::' || object_host || '::' || object_version::TEXT
  );
END
$$
LANGUAGE plpgsql;
-- party.sql
CREATE OR REPLACE FUNCTION ehr.js_party(UUID)
  RETURNS JSON AS
$$
DECLARE
  party_id ALIAS FOR $1;
BEGIN
  RETURN (
    SELECT ehr.js_party_identified(name,
                                   ehr.js_party_ref(party_ref_value, party_ref_scheme, party_ref_namespace, party_ref_type))
    FROM ehr.party_identified
    WHERE id = party_id
  );
END
$$
LANGUAGE plpgsql;
-- party_identified.sql
CREATE OR REPLACE FUNCTION ehr.js_party_identified(TEXT, JSON)
  RETURNS JSON AS
$$
DECLARE
  name_value ALIAS FOR $1;
  external_ref ALIAS FOR $2;
BEGIN
  IF (external_ref IS NOT NULL) THEN
    RETURN
    json_build_object(
        '_type', 'PARTY_IDENTIFIED',
        'name', name_value,
        'external_ref', external_ref
    );
  ELSE
    RETURN
    json_build_object(
        '_type', 'PARTY_IDENTIFIED',
        'name', name_value
    );
  END IF;
END
$$
LANGUAGE plpgsql;
-- party_ref.sql
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
        'namespace', namespace
    );
  END IF;
END
$$
LANGUAGE plpgsql;

-- ehr_status
CREATE OR REPLACE FUNCTION ehr.js_ehr_status(UUID)
  RETURNS JSON AS
$$
DECLARE
  ehr_uuid ALIAS FOR $1;
BEGIN
  RETURN (
    WITH ehr_status_data AS (
      SELECT
        status.other_details as other_details,
        status.party as subject,
        status.is_queryable as is_queryable,
        status.is_modifiable as is_modifiable,
        status.sys_transaction as time_created
      FROM ehr.status
      WHERE status.ehr_id = ehr_uuid
      LIMIT 1
    )
    SELECT
      jsonb_strip_nulls(
          jsonb_build_object(
              '_type', 'EHR_STATUS',
              'subject', ehr.js_party(subject),
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