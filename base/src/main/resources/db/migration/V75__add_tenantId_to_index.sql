/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- drop all index

-- V2
DROP INDEX ehr_status_history;  
DROP INDEX ehr_composition_history;  
DROP INDEX ehr_event_context_history;  
DROP INDEX ehr_participation_history;  
DROP INDEX ehr_entry_history;  
DROP INDEX ehr_compo_xref;  
DROP INDEX gin_entry_path_idx;  
DROP INDEX template_entry_idx;  
DROP INDEX IF EXISTS entry_composition_id_idx;  
DROP INDEX composition_composer_idx;  
DROP INDEX composition_ehr_idx;  
DROP INDEX IF EXISTS status_ehr_idx;  
DROP INDEX status_party_idx;  
DROP INDEX context_facility_idx;  
DROP INDEX context_composition_id_idx;  
DROP INDEX context_setting_idx;  

-- V8
DROP INDEX folder_in_contribution_idx;  
DROP INDEX folder_hierarchy_in_contribution_idx;  
DROP INDEX fki_folder_hierarchy_parent_fk;
DROP INDEX obj_ref_in_contribution_idx;
DROP INDEX folder_hist_idx;
 
-- V59
DROP INDEX party_identified_party_type_idx;  
DROP INDEX party_identified_party_ref_idx;

-- V60
-- DROP INDEX territory_code_index;  
DROP INDEX context_participation_index;

-- V61
DROP INDEX ehr_subject_id_index;
  
-- V63
DROP INDEX ehr_folder_idx;

-- V64
DROP INDEX attestation_reference_idx;  
DROP INDEX attested_view_attestation_idx;  
DROP INDEX compo_xref_child_idx;  
DROP INDEX composition_history_ehr_idx;  
DROP INDEX contribution_ehr_idx;  
DROP INDEX entry_history_composition_idx;  
DROP INDEX event_context_history_composition_idx;  
DROP INDEX folder_history_contribution_idx;  
DROP INDEX folder_items_contribution_idx;  
DROP INDEX folder_items_history_contribution_idx;  
DROP INDEX folder_hierarchy_history_contribution_idx;  
DROP INDEX object_ref_history_contribution_idx;  
DROP INDEX participation_history_event_context_idx;  
DROP INDEX status_history_ehr_idx;  

-- V67
DROP INDEX ehr_identifier_party_idx;

-- V69
ALTER TABLE ehr.entry DROP CONSTRAINT entry_composition_id_key;
ALTER TABLE ehr.status DROP CONSTRAINT status_ehr_id_key;  

-- V70
DROP INDEX party_identified_namespace_value_idx;

-- V71
DROP INDEX identifier_value_idx;  

-- create all idex again

-- V2
CREATE INDEX ehr_status_history ON ehr.status_history USING BTREE (id, namespace);
CREATE INDEX ehr_composition_history ON ehr.composition_history USING BTREE (id, namespace);
CREATE INDEX ehr_event_context_history ON ehr.event_context_history USING BTREE (id, namespace);
CREATE INDEX ehr_participation_history ON ehr.participation_history USING BTREE (id, namespace);
CREATE INDEX ehr_entry_history ON ehr.entry_history USING BTREE (id, namespace);
CREATE INDEX ehr_compo_xref ON ehr.compo_xref USING BTREE (master_uuid, namespace);

CREATE INDEX template_entry_idx ON ehr.entry (template_id, namespace);
CREATE INDEX composition_composer_idx ON ehr.composition (composer, namespace);
CREATE INDEX composition_ehr_idx ON ehr.composition (ehr_id, namespace);
CREATE INDEX status_party_idx ON ehr.status (party, namespace);
CREATE INDEX context_facility_idx ON ehr.event_context (facility, namespace);
CREATE INDEX context_setting_idx ON ehr.event_context (setting, namespace);

-- V8
CREATE INDEX folder_in_contribution_idx ON ehr.folder USING btree (in_contribution, namespace) TABLESPACE pg_default;
CREATE INDEX folder_hierarchy_in_contribution_idx ON ehr.folder_hierarchy USING btree (in_contribution, namespace) TABLESPACE pg_default;
CREATE INDEX fki_folder_hierarchy_parent_fk ON ehr.folder_hierarchy USING btree (parent_folder, namespace) TABLESPACE pg_default;
CREATE INDEX obj_ref_in_contribution_idx ON ehr.object_ref USING btree (in_contribution, namespace) TABLESPACE pg_default;
CREATE INDEX folder_hist_idx ON ehr.folder_items_history USING btree (folder_id, object_ref_id, in_contribution, namespace) TABLESPACE pg_default;

-- V59
CREATE INDEX party_identified_party_type_idx ON ehr.party_identified(party_type, name, namespace);  
CREATE INDEX party_identified_party_ref_idx ON ehr.party_identified(party_ref_namespace, party_ref_scheme, party_ref_value, namespace);

-- V61
CREATE INDEX IF NOT EXISTS ehr_subject_id_index ON ehr.party_identified(
    jsonb_extract_path_text(cast("ehr"."js_party_ref"(
        ehr.party_identified.party_ref_value,
        ehr.party_identified.party_ref_scheme,
        ehr.party_identified.party_ref_namespace,
        ehr.party_identified.party_ref_type
    ) as jsonb),'id','value')
    , namespace
);

-- V63
CREATE UNIQUE INDEX ehr_folder_idx ON ehr.ehr(directory, namespace);

-- V64
CREATE INDEX IF NOT EXISTS attestation_reference_idx ON ehr.attestation (reference, namespace);
CREATE INDEX IF NOT EXISTS attested_view_attestation_idx ON ehr.attested_view (attestation_id, namespace);
CREATE INDEX IF NOT EXISTS compo_xref_child_idx ON ehr.compo_xref (child_uuid, namespace);
CREATE INDEX IF NOT EXISTS composition_history_ehr_idx ON ehr.composition_history (ehr_id, namespace);
CREATE INDEX IF NOT EXISTS contribution_ehr_idx ON ehr.contribution (ehr_id, namespace);
CREATE INDEX IF NOT EXISTS entry_history_composition_idx ON ehr.entry_history (composition_id, namespace);
CREATE INDEX IF NOT EXISTS event_context_history_composition_idx ON ehr.event_context_history (composition_id, namespace);
CREATE INDEX IF NOT EXISTS folder_history_contribution_idx ON ehr.folder_history (in_contribution, namespace);
CREATE INDEX IF NOT EXISTS folder_items_contribution_idx ON ehr.folder_items (in_contribution, namespace);
CREATE INDEX IF NOT EXISTS folder_items_history_contribution_idx ON ehr.folder_items_history (in_contribution, namespace);
CREATE INDEX IF NOT EXISTS folder_hierarchy_history_contribution_idx ON ehr.folder_hierarchy_history (in_contribution, namespace);
CREATE INDEX IF NOT EXISTS object_ref_history_contribution_idx ON ehr.object_ref_history (in_contribution, namespace);
CREATE INDEX IF NOT EXISTS participation_history_event_context_idx ON ehr.participation_history (event_context, namespace);
CREATE INDEX IF NOT EXISTS status_history_ehr_idx ON ehr.status_history (ehr_id, namespace);

-- V67
CREATE INDEX ehr_identifier_party_idx ON ehr.identifier(party, namespace);
CREATE UNIQUE INDEX entry_composition_id_idx on ehr.entry(composition_id, namespace);

-- V68
CREATE UNIQUE INDEX context_composition_id_idx ON ehr.event_context(composition_id, namespace);  
CREATE UNIQUE INDEX status_ehr_idx ON ehr.status(ehr_id, namespace);

-- V69
ALTER TABLE ehr.entry ADD CONSTRAINT entry_composition_id_key UNIQUE USING INDEX entry_composition_id_idx;
ALTER TABLE ehr.status ADD CONSTRAINT status_ehr_id_key UNIQUE USING INDEX status_ehr_idx;

-- V70
CREATE INDEX party_identified_namespace_value_idx ON party_identified(party_ref_namespace, party_ref_value, namespace);

-- V71
create index identifier_value_idx on ehr.identifier (id_value, namespace);