DROP FUNCTION IF EXISTS ehr.js_dv_date_time(TIMESTAMP WITH TIME ZONE, TEXT);

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
        '@class', 'DV_DATE_TIME',
        'value', timezone(time_zone, date_time::timestamp)
      );
END
$$
  LANGUAGE plpgsql;