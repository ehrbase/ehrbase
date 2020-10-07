-- ensures that date/time handling is the same for time with or without timezone
CREATE OR REPLACE FUNCTION ehr.js_dv_date_time(datetime TIMESTAMP, timezone TEXT)
    RETURNS JSON AS
$$
BEGIN
    RETURN ehr.js_dv_date_time(datetime::TIMESTAMPTZ, timezone);
END
$$
LANGUAGE plpgsql;