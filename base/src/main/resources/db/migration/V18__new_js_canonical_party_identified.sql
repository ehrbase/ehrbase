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
        party_identified.party_ref_type as type
      FROM ehr.party_identified
      WHERE party_identified.id = refid
    )
    SELECT
      jsonb_strip_nulls(
          jsonb_build_object (
              '_type', 'PARTY_IDENTIFIED',
              'name', party_values.name,
              'identifiers',
              jsonb_build_array(
                  jsonb_build_object(
                      '_type', 'DV_IDENTIFIER',
                      'id',refid
                    )
                ),
              'external_ref', jsonb_build_object(
                  '_type', 'PARTY_REF',
                  'namespace', party_values.namespace,
                  'type', party_values.type,
                  'id', ehr.js_canonical_generic_id(party_values.scheme, party_values.value)
                )
            )
        )
    FROM party_values
  );
END
$$
  LANGUAGE plpgsql;