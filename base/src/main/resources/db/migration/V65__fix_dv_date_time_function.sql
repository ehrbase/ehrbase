-- Removes 'Z' when timezone is NULL

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
        time_zone := '';
END IF;

RETURN
    json_build_object(
            '_type', 'DV_DATE_TIME',
            'value',to_char(date_time, 'YYYY-MM-DD"T"HH24:MI:SS.MS"'||time_zone||'"')
        );
END
$$
LANGUAGE plpgsql;