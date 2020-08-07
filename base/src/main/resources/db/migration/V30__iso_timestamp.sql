-- this is to fix the timezone drift and provide the correct encoding
CREATE OR REPLACE FUNCTION ehr.js_dv_date_time(TIMESTAMPTZ, TEXT)
    RETURNS JSON AS
$$
DECLARE
    date_time ALIAS FOR $1;
    time_zone ALIAS FOR $2;
    value_date_time TEXT;
BEGIN

    IF (date_time IS NULL)
    THEN
        RETURN NULL;
    END IF;

    IF (time_zone IS NULL)
    THEN
        time_zone := 'UTC';
    END IF;

    value_date_time := timezone('UTC', timezone('UTC',date_time::TIMESTAMPTZ) AT TIME ZONE time_zone);

    RETURN
        json_build_object(
                '_type', 'DV_DATE_TIME',
                'value', ehr.iso_timestamp(value_date_time::TIMESTAMPTZ)||time_zone
            );
END
$$
    LANGUAGE plpgsql;