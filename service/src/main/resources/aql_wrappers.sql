CREATE OR REPLACE FUNCTION aql_value_dv_coded_text(object_struct json, field_name text) RETURNS text AS $$

BEGIN
	raw_archetype := split_part(raw_archetype,'and name/value=', 1);
	raw_archetype := replace(raw_archetype, '-', '_');
	raw_archetype := replace(raw_archetype, '.', '_');
	raw_archetype := trim(trailing ' ' from raw_archetype);

	return raw_archetype;
END

$$ LANGUAGE plpgsql;