-- Add support for attribute name and archetype_node_id in ehr_status

-- modify table ehr.status to add the missing attributes

ALTER TABLE ehr.status
  ADD COLUMN archetype_node_id TEXT NOT NULL DEFAULT 'openEHR-EHR-EHR_STATUS.generic.v1',
  ADD COLUMN name ehr.dv_coded_text NOT NULL DEFAULT ('EHR Status',NULL,NULL,NULL,NULL)::ehr.dv_coded_text ;

-- modify function to return ehr_status canonical json to support the new attributes
CREATE OR REPLACE FUNCTION ehr.js_ehr_status(UUID)
  RETURNS JSON AS
$$
DECLARE
  ehr_uuid ALIAS FOR $1;
BEGIN
  RETURN (
    WITH ehr_status_data AS (
      SELECT
        status.other_details as other_details,
        status.party as subject,
        status.is_queryable as is_queryable,
        status.is_modifiable as is_modifiable,
        status.sys_transaction as time_created,
        status.name as status_name,
        status.archetype_node_id as archetype_node_id
      FROM ehr.status
      WHERE status.ehr_id = ehr_uuid
      LIMIT 1
    )
    SELECT
      jsonb_strip_nulls(
          jsonb_build_object(
              '_type', 'EHR_STATUS',
              'archetype_node_id', archetype_node_id,
              'name', status_name,
              'subject', ehr.js_party(subject),
              'is_queryable', is_queryable,
              'is_modifiable', is_modifiable,
              'other_details', other_details
            )
        )
    FROM ehr_status_data
  );
END
$$
  LANGUAGE plpgsql;