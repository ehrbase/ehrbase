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

ALTER TABLE ehr.ehr ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.ehr FORCE ROW LEVEL SECURITY;

ALTER TABLE ehr.entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry FORCE ROW LEVEL SECURITY;

ALTER TABLE ehr.entry_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.entry_history FORCE ROW LEVEL SECURITY;

ALTER TABLE ehr.composition ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.composition FORCE ROW LEVEL SECURITY;

ALTER TABLE ehr.composition_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.composition_history FORCE ROW LEVEL SECURITY;

ALTER TABLE ehr.event_context ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context FORCE ROW LEVEL SECURITY;

ALTER TABLE ehr.event_context_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context_history FORCE ROW LEVEL SECURITY;

-- create policies

CREATE POLICY ehr_policy_all ON ehr.ehr FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.entry FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.entry_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.composition FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.composition_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.event_context FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));
CREATE POLICY ehr_policy_all ON ehr.event_context_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));

-- CREATE POLICY ehr_policy ON ehr.ehr FOR SELECT USING (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy ON ehr.entry FOR SELECT USING (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy ON ehr.entry_history FOR SELECT USING (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy ON ehr.composition FOR SELECT USING (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy ON ehr.composition_history FOR SELECT USING (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy ON ehr.event_context FOR SELECT USING (namespace = current_setting('ehrbase.current_tenant'));
-- CREATE POLICY ehr_policy ON ehr.event_context_history FOR SELECT USING (namespace = current_setting('ehrbase.current_tenant'));
