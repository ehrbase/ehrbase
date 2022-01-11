DO
$$
    DECLARE
        current_value TEXT;
    BEGIN
        SELECT setting FROM pg_settings WHERE name = 'IntervalStyle' INTO current_value;

        IF current_value != 'iso_8601'
        THEN
            RAISE EXCEPTION 'Your database is not properly configured, IntervalStyle setting % must be change to iso_8601', current_value;
        END IF;
    END
$$;