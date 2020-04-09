ALTER TABLE ehr.status_history
  ADD COLUMN archetype_node_id TEXT NOT NULL DEFAULT 'openEHR-EHR-EHR_STATUS.generic.v1',
  ADD COLUMN name ehr.dv_coded_text NOT NULL DEFAULT ('EHR Status',NULL,NULL,NULL,NULL)::ehr.dv_coded_text;