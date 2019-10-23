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


CREATE OR REPLACE FUNCTION ehr.js_canonical_hier_object_id(TEXT)
  RETURNS JSON AS
$$
DECLARE
  value ALIAS FOR $1;
BEGIN
  RETURN
    json_build_object(
        '_type', 'HIER_OBJECT_ID',
        'value', value
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
            'id', ehr.js_generic_id(scheme, id)
          )
      );
END
$$
LANGUAGE plpgsql;


