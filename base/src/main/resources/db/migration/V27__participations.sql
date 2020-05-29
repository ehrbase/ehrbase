-- extension for DvInterval<DvDateTime>
ALTER TABLE ehr.participation
  RENAME COLUMN start_time TO time_lower;

ALTER TABLE ehr.participation
  RENAME COLUMN start_time_tzid TO time_lower_tz;

ALTER TABLE ehr.participation
  ADD COLUMN time_upper TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE ehr.participation
  ADD COLUMN time_upper_tz TEXT;

-- ditto for history
ALTER TABLE ehr.participation_history
  RENAME COLUMN start_time TO time_lower;

ALTER TABLE ehr.participation_history
  RENAME COLUMN start_time_tzid TO time_lower_tz;

ALTER TABLE ehr.participation_history
  ADD COLUMN time_upper TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE ehr.participation_history
  ADD COLUMN time_upper_tz TEXT;

-- used to convert existing mode as a proper ehr.dv_coded_text type
CREATE OR REPLACE FUNCTION ehr.migrate_participation_mode(mode TEXT)
  RETURNS ehr.dv_coded_text AS
$$
BEGIN
  RETURN (
    WITH dv_coded_text_attributes AS (
      WITH mode_split AS (
        select
          regexp_split_to_array((
            (regexp_split_to_array(mode,'{|}'))[2]), ',')
            as arr
      )
      select
        (regexp_split_to_array(arr[1],'='))[2] as code_string,
        (regexp_split_to_array(arr[2],'='))[2] as terminology_id,
        (regexp_split_to_array(arr[3],'='))[2] as value
      from mode_split
    )
    select (value, (terminology_id, code_string)::ehr.code_phrase,null,null,null)::ehr.dv_coded_text from dv_coded_text_attributes
  );
END
$$
  LANGUAGE plpgsql;


ALTER TABLE ehr.participation
  ALTER COLUMN mode TYPE ehr.dv_coded_text
  USING ehr.migrate_participation_mode(mode);

ALTER TABLE ehr.participation_history
  ALTER COLUMN mode TYPE ehr.dv_coded_text
    USING ehr.migrate_participation_mode(mode);

--
CREATE OR REPLACE FUNCTION ehr.js_code_phrase(codephrase ehr.code_phrase)
  RETURNS JSON AS
$$
BEGIN
  RETURN
    json_build_object(
        '_type', 'CODE_PHRASE',
        'terminology_id',
        json_build_object(
            '_type', 'TERMINOLOGY_ID',
            'value', codephrase.terminology_id_value
          ),
        'code_string', codephrase.code_string
      );
END
$$
  LANGUAGE plpgsql;

-- from PR #232 TERM_MAPPING
CREATE OR REPLACE FUNCTION ehr.js_dv_coded_text_inner(dvcodedtext ehr.dv_coded_text)
  RETURNS JSON AS
$$
BEGIN
  RETURN
    json_build_object(
        '_type', 'DV_CODED_TEXT',
        'value', dvcodedtext.value,
        'defining_code', ehr.js_code_phrase(dvcodedtext.defining_code)
      );
END
$$
  LANGUAGE plpgsql;


-- returns an array of canonical participations
CREATE OR REPLACE FUNCTION ehr.js_participations(event_context_id UUID)
  RETURNS JSONB[] AS
$$
DECLARE
  item JSONB;
  arr JSONB[];
  participation_data RECORD;
BEGIN

  FOR participation_data IN
    SELECT
      participation.performer as performer,
      participation.function as function,
      participation.mode as mode,
      participation.time_lower,
      participation.time_lower_tz,
      participation.time_upper,
      participation.time_upper_tz
    FROM ehr.participation
    WHERE event_context = event_context_id
    LOOP
      item :=
          jsonb_strip_nulls(
              jsonb_build_object(
                  '_type', 'PARTICIPATION',
                  'function', participation_data.function,
                  'performer', ehr.js_canonical_party_identified(participation_data.performer),
                  'mode', ehr.js_dv_coded_text_inner(participation_data.mode),
                  'time', (SELECT (
                                    CASE WHEN (participation_data.time_lower IS NOT NULL OR participation_data.time_upper IS NOT NULL) THEN
                                           jsonb_build_object(
                                               '_type', 'DV_INTERVAL',
                                               'lower', ehr.js_dv_date_time(participation_data.time_lower, participation_data.time_lower_tz),
                                               'upper', ehr.js_dv_date_time(participation_data.time_upper, participation_data.time_upper_tz)
                                             )
                                         ELSE
                                           NULL
                                      END
                                    )
                  )
                )
            );
      arr := array_append(arr, item);
    END LOOP;
  RETURN arr;
END
$$
  LANGUAGE plpgsql;

-- returns a canonical representation of participations
CREATE OR REPLACE FUNCTION ehr.js_canonical_participations(context_id UUID)
  RETURNS JSON AS
$$
BEGIN
  RETURN (SELECT  jsonb_array_elements(jsonb_build_array(ehr.js_participations(context_id))));
END
$$
  LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.js_context(UUID)
  RETURNS JSON AS
$$
DECLARE
  context_id ALIAS FOR $1;
BEGIN

  IF (context_id IS NULL)
  THEN
    RETURN NULL;
  ELSE
    RETURN (
      WITH context_attributes AS (
        SELECT
          start_time,
          start_time_tzid,
          end_time,
          end_time_tzid,
          facility,
          location,
          other_context,
          setting
        FROM ehr.event_context
        WHERE id = context_id
      )
      SELECT jsonb_strip_nulls(
                 jsonb_build_object(
                     '_type', 'EVENT_CONTEXT',
                     'start_time', ehr.js_dv_date_time(start_time, start_time_tzid),
                     'end_time', ehr.js_dv_date_time(end_time, end_time_tzid),
                     'location', location,
                     'health_care_facility', ehr.js_party(facility),
                     'setting', ehr.js_context_setting(setting),
                     'other_context',other_context,
                     'participations', ehr.js_participations(context_id)
                   )
               )
      FROM context_attributes
    );
  END IF;
END
$$
  LANGUAGE plpgsql;