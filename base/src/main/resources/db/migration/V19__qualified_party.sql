-- modification of table PARTY_IDENTIFIED with added field as required
-- NB. we keep this table name as to avoid heavy refactoring of the code base referencing this table.
-- create an enum type to qualify parties
create type ehr.party_type as enum('party_identified', 'party_self', 'party_related');

-- UDT for CODE_PHRASE
create type ehr.code_phrase as  (
  terminology_id_value text,
  code_string text
  );

-- UDT for DV_CODED_TEXT
create type ehr.dv_coded_text as (
  value text,
  defining_code ehr.code_phrase,
  formatting text,
  -- mappings: has forward usage of type!
  language ehr.code_phrase,
  encoding ehr.code_phrase
  );

-- add support of qualification (type) and relationship for party_type == party_related
ALTER TABLE ehr.party_identified
  ADD COLUMN party_type ehr.party_type DEFAULT 'party_identified',
  ADD COLUMN relationship ehr.dv_coded_text,
  ADD CONSTRAINT party_related_check check (
    (CASE
        WHEN party_type = 'party_related' THEN relationship IS NOT NULL
      END)
  );

-- update corresponding canonical functions
-- TODO: add proper support for PARTY_RELATED

CREATE OR REPLACE FUNCTION ehr.json_party_identified(name TEXT, refid UUID, namespace TEXT, ref_type TEXT, scheme TEXT, id_value TEXT)
  RETURNS json AS
$$
DECLARE
  json_party_struct JSON;
BEGIN
  SELECT
    jsonb_strip_nulls(
        jsonb_build_object (
            '_type', 'PARTY_IDENTIFIED',
            'name', name,
            'identifiers',
            jsonb_build_array(
                jsonb_build_object(
                    '_type', 'DV_IDENTIFIER',
                    'id',refid
                  )
              ),
            'external_ref', jsonb_build_object(
                '_type', 'PARTY_REF',
                'namespace', namespace,
                'type', ref_type,
                'id', ehr.js_canonical_generic_id(scheme, id_value)
              )
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
        party_identified.relationship as relationship
      FROM ehr.party_identified
      WHERE party_identified.id = refid
    )
    SELECT
      CASE
        WHEN party_values.party_type = 'party_identified'
          THEN
          ehr.json_party_identified(party_values.name, refid, party_values.namespace, party_values.ref_type, party_values.scheme, party_values.value)::json

        WHEN party_values.party_type = 'party_self'
          THEN
          jsonb_build_object (
              '_type', 'PARTY_SELF'
            )::json
        WHEN party_values.party_type = 'party_related'
          THEN
          ehr.json_party_identified(party_values.name, refid, party_values.namespace, party_values.ref_type, party_values.scheme, party_values.value)::json
        END
    FROM party_values
  );
END
$$
  LANGUAGE plpgsql;