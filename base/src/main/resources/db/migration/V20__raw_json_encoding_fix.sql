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
                '_type', 'DV_DATE_TIME',
                'value', timezone(time_zone, date_time::timestamp)
            );
END
$$
    LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION ehr.js_context(UUID)
    RETURNS JSON AS
$$
DECLARE
    context_id ALIAS FOR $1;
    json_context_query TEXT;
    json_context       JSON;
    v_start_time       TIMESTAMP;
    v_start_time_tzid  TEXT;
    v_end_time         TIMESTAMP;
    v_end_time_tzid    TEXT;
    v_facility         UUID;
    v_location         TEXT;
    v_other_context    JSON;
    v_setting          UUID;
BEGIN

    IF (context_id IS NULL)
    THEN
        RETURN NULL;
    END IF;

    -- build the query
    SELECT start_time,
           start_time_tzid,
           end_time,
           end_time_tzid,
           facility,
           location,
           other_context,
           setting
    FROM ehr.event_context
    WHERE id = context_id
    INTO v_start_time, v_start_time_tzid, v_end_time, v_end_time_tzid, v_facility, v_location, v_other_context, v_setting;

    json_context_query := ' SELECT json_build_object(
                                  ''_type'', ''EVENT_CONTEXT'',
                                  ''start_time'', ehr.js_dv_date_time(''' || v_start_time || ''',''' ||
                          v_start_time_tzid || '''),';

    IF (v_end_time IS NOT NULL)
    THEN
        json_context_query :=
                            json_context_query || '''end_date'', ehr.js_dv_date_time(''' || v_end_time || ''',''' ||
                            v_end_time_tzid ||
                            '''),';
    END IF;

    IF (v_location IS NOT NULL)
    THEN
        json_context_query := json_context_query || '''location'', ''' || v_location || ''',';
    END IF;

    IF (v_facility IS NOT NULL)
    THEN
        json_context_query := json_context_query || '''health_care_facility'', ehr.js_party(''' || v_facility || '''),';
    END IF;

    json_context_query := json_context_query || '''setting'',ehr.js_context_setting(''' || v_setting || '''))';


    --     IF (participation IS NOT NULL) THEN
    --       json_context_query := json_context_query || '''participation'', participation,';
    --     END IF;

    IF (json_context_query IS NULL)
    THEN
        RETURN NULL;
    END IF;

    EXECUTE json_context_query
        INTO STRICT json_context;

    IF (v_other_context IS NOT NULL)
    THEN
        json_context := jsonb_insert(
                json_context::JSONB,
                '{other_context}', v_other_context::JSONB
            )::JSON;
    END IF;

    RETURN json_context;
END
$$
    LANGUAGE plpgsql;