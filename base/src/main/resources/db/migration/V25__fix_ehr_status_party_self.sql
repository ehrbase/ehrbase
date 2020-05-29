-- supported OBJECT_ID subtypes
create type ehr.party_ref_id_type as enum('generic_id', 'object_version_id', 'hier_object_id', 'undefined');
alter table ehr.party_identified add column object_id_type ehr.party_ref_id_type default 'generic_id';

-- caused an exception when inserting a UDT for relationship
alter table ehr.party_identified drop constraint party_related_check;

CREATE OR REPLACE FUNCTION ehr.js_party_self_identified(TEXT, JSON)
  RETURNS JSON AS
$$
DECLARE
  name_value ALIAS FOR $1;
  external_ref ALIAS FOR $2;
BEGIN
  IF (external_ref IS NOT NULL) THEN
    RETURN
      json_build_object(
          '_type', 'PARTY_SELF',
          'external_ref', external_ref
        );
  ELSE
    RETURN
      json_build_object(
          '_type', 'PARTY_SELF'
        );
  END IF;
END
$$
  LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.js_party_self(UUID)
  RETURNS JSON AS
$$
DECLARE
  party_id ALIAS FOR $1;
BEGIN
  RETURN (
    SELECT ehr.js_party_self_identified(name,
                                   ehr.js_party_ref(party_ref_value, party_ref_scheme, party_ref_namespace, party_ref_type))
    FROM ehr.party_identified
    WHERE id = party_id
  );
END
$$
  LANGUAGE plpgsql;

-- modify function to return ehr_status canonical json to support the new attributes
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
              'subject', ehr.js_party_self(subject),
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

-- =================== AQL fixes ======================================================
CREATE OR REPLACE FUNCTION ehr.js_code_phrase(codephrase ehr.code_phrase)
  RETURNS JSON AS
$$
DECLARE

BEGIN
  RETURN
    json_build_object(
        '_type', 'CODE_PHRASE',
        'terminology_id',
        json_build_object(
            '_type', 'TERMINOLOGY_ID',
            'value', codephrase.terminology_id_value
          ),
        'code_string', codephrase.code_string
      );
END
$$
  LANGUAGE plpgsql;

-- borrowed from TERM_MAPPING fix
CREATE OR REPLACE FUNCTION ehr.js_code_phrase(codephrase ehr.code_phrase)
  RETURNS JSON AS
$$
DECLARE

BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
            '_type', 'CODE_PHRASE',
            'terminology_id',
            json_build_object(
                '_type', 'TERMINOLOGY_ID',
                'value', codephrase.terminology_id_value
              ),
            'code_string', codephrase.code_string
          )
      );
END
$$
  LANGUAGE plpgsql;

-- borrowed from TERM_MAPPING fix
CREATE OR REPLACE FUNCTION ehr.js_dv_coded_text(dvcodedtext ehr.dv_coded_text)
  RETURNS JSON AS
$$
BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
            '_type', 'DV_CODED_TEXT',
            'value', dvcodedtext.value,
            'defining_code', ehr.js_code_phrase(dvcodedtext.defining_code),
            'formatting', dvcodedtext.formatting,
            'language', dvcodedtext.language,
            'encoding', dvcodedtext.encoding
          )
      );
END
$$
  LANGUAGE plpgsql;

-- OBJECT_ID
DROP FUNCTION ehr.js_canonical_generic_id(text,text);

CREATE OR REPLACE FUNCTION ehr.js_canonical_generic_id(scheme TEXT, id_value TEXT)
  RETURNS json AS
$$
BEGIN
  RETURN
    jsonb_strip_nulls(
        jsonb_build_object(
            '_type', 'GENERIC_ID',
            'value', id_value,
            'scheme', scheme
          )
      );
END
$$
  LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_canonical_hier_object_id(id_value TEXT)
  RETURNS json AS
$$
BEGIN
  RETURN
    json_build_object(
        '_type', 'HIER_OBJECT_ID',
        'value', id_value
      );
END
$$
  LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ehr.js_canonical_object_version_id(id_value TEXT)
  RETURNS json AS
$$
BEGIN
  RETURN
    json_build_object(
        '_type', 'OBJECT_VERSION_ID',
        'value', id_value
      );
END
$$
  LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_canonical_object_id(objectid_type ehr.party_ref_id_type, scheme TEXT, id_value TEXT)
  RETURNS json AS
$$
BEGIN
  RETURN (
    SELECT
      CASE
        WHEN objectid_type = 'generic_id'
          THEN
          ehr.js_canonical_generic_id(scheme, id_value)
        WHEN objectid_type = 'hier_object_id'
          THEN
          ehr.js_canonical_hier_object_id(id_value)
        WHEN objectid_type = 'object_version_id'
          THEN
          ehr.js_canonical_object_version_id(id_value)
        WHEN objectid_type = 'undefined'
          THEN
          NULL
        END
  );
END
$$
  LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.json_party_self(refid UUID, namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT, objectid_type ehr.party_ref_id_type)
  RETURNS json AS
$$
DECLARE
  json_party_struct JSON;
BEGIN
  SELECT
    jsonb_strip_nulls(
        jsonb_build_object (
            '_type', 'PARTY_SELF',
            'external_ref', jsonb_build_object(
                '_type', 'PARTY_REF',
                'namespace', namespace,
                'type', ref_type,
                'id', ehr.js_canonical_object_id(objectid_type, scheme, id_value)
              )
          )
      )
    INTO json_party_struct;
  RETURN json_party_struct;
end;
$$
  language 'plpgsql';


CREATE OR REPLACE FUNCTION ehr.json_party_identified(name TEXT, refid UUID, namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT, objectid_type ehr.party_ref_id_type)
  RETURNS json AS
$$
DECLARE
  json_party_struct JSON;
  item JSONB;
  arr JSONB[];
  identifier_attribute record;
BEGIN
  -- build an array of json object from identifiers if any
  FOR identifier_attribute IN SELECT * FROM ehr.identifier WHERE identifier.party = refid LOOP
    item := jsonb_build_object(
        '_type', 'DV_IDENTIFIER',
        'id',identifier_attribute.id_value
      );
    arr := array_append(arr, item);
  END LOOP;

  SELECT
    jsonb_strip_nulls(
        jsonb_build_object (
            '_type', 'PARTY_IDENTIFIED',
            'name', name,
            'identifiers', arr,
            'external_ref', jsonb_build_object(
                '_type', 'PARTY_REF',
                'namespace', namespace,
                'type', ref_type,
                'id', ehr.js_canonical_object_id(objectid_type, scheme, id_value)
              )
          )
      )
    INTO json_party_struct;
  RETURN json_party_struct;
end;
$$
  language 'plpgsql';



CREATE OR REPLACE FUNCTION ehr.json_party_related(name TEXT, refid UUID, namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT, objectid_type ehr.party_ref_id_type, relationship ehr.dv_coded_text)
  RETURNS json AS
$$
DECLARE
  json_party_struct JSON;
  item JSONB;
  arr JSONB[];
  identifier_attribute record;
BEGIN
  -- build an array of json object from identifiers if any
  FOR identifier_attribute IN SELECT * FROM ehr.identifier WHERE identifier.party = refid LOOP
    item := jsonb_build_object(
        '_type', 'DV_IDENTIFIER',
        'id',identifier_attribute.id_value
      );
    arr := array_append(arr, item);
  END LOOP;

  SELECT
    jsonb_strip_nulls(
        jsonb_build_object (
            '_type', 'PARTY_RELATED',
            'name', name,
            'identifiers', arr,
            'external_ref', jsonb_build_object(
                '_type', 'PARTY_REF',
                'namespace', namespace,
                'type', ref_type,
                'id', ehr.js_canonical_object_id(objectid_type, scheme, id_value)
              ),
            'relationship', ehr.js_dv_coded_text(relationship)
          )
      )
    INTO json_party_struct;
  RETURN json_party_struct;
end;
$$
  language 'plpgsql';

CREATE OR REPLACE FUNCTION ehr.js_canonical_party_identified(refid UUID)
  RETURNS json AS
$$
BEGIN
  RETURN (
    WITH party_values AS (
      SELECT
        party_identified.name as name,
        party_identified.party_ref_value as value,
        party_identified.party_ref_scheme as scheme,
        party_identified.party_ref_namespace as namespace,
        party_identified.party_ref_type as ref_type,
        party_identified.party_type as party_type,
        party_identified.relationship as relationship,
        party_identified.object_id_type as objectid_type
      FROM ehr.party_identified
      WHERE party_identified.id = refid
    )
    SELECT
      CASE
        WHEN party_values.party_type = 'party_identified'
          THEN
          ehr.json_party_identified(party_values.name, refid, party_values.namespace, party_values.ref_type, party_values.scheme, party_values.value, party_values.objectid_type)::json
        WHEN party_values.party_type = 'party_self'
          THEN
          ehr.json_party_self(refid, party_values.namespace, party_values.ref_type, party_values.scheme, party_values.value, party_values.objectid_type)::json
        WHEN party_values.party_type = 'party_related'
          THEN
          ehr.json_party_related(party_values.name, refid, party_values.namespace, party_values.ref_type, party_values.scheme, party_values.value, party_values.objectid_type, relationship)::json
        END
    FROM party_values
  );
END
$$
  LANGUAGE plpgsql;


-- fix to support composition with no content
CREATE OR REPLACE FUNCTION ehr.js_composition(composition_uuid UUID, server_node_id TEXT)
  RETURNS JSON AS
$$
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
              'archetype_details', ehr.js_archetype_details(entry_content.archetype_id, entry_content.template_id),
              'archetype_node_id', entry_content.archetype_id,
              'uid', ehr.js_object_version_id(ehr.composition_uid(entry_content.composition_id, server_node_id)),
              'language', ehr.js_code_phrase(language, 'ISO_639-1'),
              'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
              'composer', ehr.js_party(composer),
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