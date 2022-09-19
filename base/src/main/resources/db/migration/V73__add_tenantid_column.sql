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

-- create tenant table

CREATE TABLE tenant (
    id UUID primary key DEFAULT ext.uuid_generate_v4(),
    tenant_id TEXT,
    tenant_name TEXT
);

ALTER TABLE ehr.tenant ADD UNIQUE (tenant_id);
ALTER TABLE ehr.tenant ADD UNIQUE (tenant_name);

INSERT INTO ehr.tenant (
    tenant_id,
    tenant_name
) VALUES (
    '1f332a66-0e57-11ed-861d-0242ac120002',
    'default_tenant'
);

-- disable all trigger

-- add namespace column to all non system tables

ALTER TABLE ehr.access ADD namespace TEXT;
ALTER TABLE ehr.attestation ADD namespace TEXT;
ALTER TABLE ehr.attestation_ref ADD namespace TEXT;
ALTER TABLE ehr.attested_view ADD namespace TEXT;
ALTER TABLE ehr.audit_details ADD namespace TEXT;
ALTER TABLE ehr.compo_xref ADD namespace TEXT;
-- ALTER TABLE ehr.concept ADD namespace TEXT;
ALTER TABLE ehr.contribution ADD namespace TEXT;
ALTER TABLE ehr.folder ADD namespace TEXT;
ALTER TABLE ehr.folder_hierarchy ADD namespace TEXT;
ALTER TABLE ehr.folder_hierarchy_history ADD namespace TEXT;
ALTER TABLE ehr.folder_history ADD namespace TEXT;
ALTER TABLE ehr.folder_items ADD namespace TEXT;
ALTER TABLE ehr.folder_items_history ADD namespace TEXT;
ALTER TABLE ehr.heading ADD namespace TEXT;
ALTER TABLE ehr.identifier ADD namespace TEXT;
-- ALTER TABLE ehr.language ADD namespace TEXT;
ALTER TABLE ehr.object_ref ADD namespace TEXT;
ALTER TABLE ehr.object_ref_history ADD namespace TEXT;
ALTER TABLE ehr.participation ADD namespace TEXT;
ALTER TABLE ehr.participation_history ADD namespace TEXT;
ALTER TABLE ehr.party_identified ADD namespace TEXT;
ALTER TABLE ehr.status ADD namespace TEXT;
ALTER TABLE ehr.status_history ADD namespace TEXT;
ALTER TABLE ehr.stored_query ADD namespace TEXT;
ALTER TABLE ehr.template_store ADD namespace TEXT;
ALTER TABLE ehr.terminology_provider ADD namespace TEXT;
ALTER TABLE ehr.session_log ADD namespace TEXT;
ALTER TABLE ehr.ehr ADD namespace TEXT;
ALTER TABLE ehr.entry ADD namespace TEXT;
ALTER TABLE ehr.entry_history ADD namespace TEXT;
ALTER TABLE ehr.composition ADD namespace TEXT;
ALTER TABLE ehr.composition_history ADD namespace TEXT;
ALTER TABLE ehr.event_context ADD namespace TEXT;
ALTER TABLE ehr.event_context_history ADD namespace TEXT;

-- insert dummy tenant

UPDATE ehr.access SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.attestation SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.attestation_ref SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.attested_view SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.audit_details SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.compo_xref SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
-- UPDATE ehr.concept SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.contribution SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.folder SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.folder_hierarchy SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.folder_hierarchy_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.folder_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.folder_items SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.folder_items_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.heading SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.identifier SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
-- UPDATE ehr.language SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.object_ref SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.object_ref_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.participation SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.participation_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.party_identified SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.status SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.status_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.stored_query SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.template_store SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.terminology_provider SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.session_log SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.ehr SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.entry SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.entry_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.composition SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.composition_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.event_context SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;
UPDATE ehr.event_context_history SET namespace = '1f332a66-0e57-11ed-861d-0242ac120002' WHERE true;

-- enable all trigger

-- change unique constraint on template_store

ALTER TABLE ehr.template_store DROP CONSTRAINT template_store_template_id_key;

ALTER TABLE ehr.template_store ADD UNIQUE (template_id, namespace);
