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

-- enable RLS

ALTER TABLE ehr.access ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.attestation ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.attestation_ref ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.attested_view ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.audit_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.compo_xref ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE ehr.concept ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.contribution ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_hierarchy ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_hierarchy_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_items_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.heading ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.identifier ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE ehr.language ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.object_ref ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.object_ref_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.participation ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.participation_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.party_identified ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.status ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.stored_query ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.template_store ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.terminology_provider ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.session_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.ehr ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.composition ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.composition_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context_history ENABLE ROW LEVEL SECURITY;

ALTER TABLE ehr.access FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.attestation FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.attestation_ref FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.attested_view FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.audit_details FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.compo_xref FORCE ROW LEVEL SECURITY;
-- ALTER TABLE ehr.concept FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.contribution FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_hierarchy FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_hierarchy_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_items FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.folder_items_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.heading FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.identifier FORCE ROW LEVEL SECURITY;
-- ALTER TABLE ehr.language FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.object_ref FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.object_ref_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.participation FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.participation_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.party_identified FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.status FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.status_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.stored_query FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.template_store FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.terminology_provider FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.session_log FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.ehr FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.composition FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.composition_history FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context FORCE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context_history FORCE ROW LEVEL SECURITY;

-- create policies

CREATE POLICY ehr_policy_all ON ehr.access FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.attestation FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.attestation_ref FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.attested_view FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.audit_details FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.compo_xref FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy_all ON ehr.concept FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.contribution FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.folder FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.folder_hierarchy FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.folder_hierarchy_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.folder_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.folder_items FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.folder_items_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.heading FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.identifier FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy_all ON ehr.language FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.object_ref FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.object_ref_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.participation FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.participation_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.party_identified FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.status FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.status_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.stored_query FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.template_store FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.terminology_provider FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.session_log FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.ehr FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.entry FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.entry_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.composition FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.composition_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.event_context FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.event_context_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));

