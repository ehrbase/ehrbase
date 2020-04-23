-- convert to lower snake case
CREATE OR REPLACE FUNCTION ehr.camel_to_snake(literal TEXT)
  RETURNS TEXT AS
$$
DECLARE
  out_literal TEXT := '';
  literal_size INT;
  char_at TEXT;
  ndx INT;
BEGIN
  literal_size := length(literal);
  if (literal_size = 0) then
    return literal;
  end if;
  ndx = 1;
  while ndx <= literal_size loop
    char_at := substr(literal, ndx , 1);
    if (char_at ~ '[A-Z]') then
      if (ndx > 1 AND substr(literal, ndx - 1, 1) <> '<') then
        out_literal = out_literal || '_';
      end if;
      out_literal = out_literal || lower(char_at);
    else
      out_literal = out_literal || char_at;
    end if;
    ndx := ndx + 1;
  end loop;
  out_literal := replace(replace(replace(out_literal, 'u_r_i', 'uri'), 'i_d', 'id'), 'i_s_m', 'ism');
  return out_literal;
END
$$
  LANGUAGE plpgsql;

-- add the _type into an element value block
CREATE OR REPLACE FUNCTION ehr.js_typed_element_value(JSONB)
  RETURNS JSONB AS
$$
DECLARE
  element_value ALIAS FOR $1;
BEGIN
  RETURN (
    SELECT
      jsonb_strip_nulls(
            (element_value #>>'{/value}')::jsonb ||
            jsonb_build_object(
                '_type',
                upper(ehr.camel_to_snake(element_value #>>'{/$CLASS$}'))
              )
        )
  );
END
$$
  LANGUAGE plpgsql;