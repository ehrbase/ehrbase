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


-- add namespace column to all non system tables

ALTER TABLE ehr.access ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.attestation ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.attestation_ref ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.attested_view ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.audit_details ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.compo_xref ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
-- ALTER TABLE ehr.concept ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.contribution ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.folder ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.folder_hierarchy ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.folder_hierarchy_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.folder_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.folder_items ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.folder_items_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.heading ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.identifier ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
-- ALTER TABLE ehr.language ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.object_ref ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.object_ref_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.participation ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.participation_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.party_identified ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.status ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.status_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.stored_query ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.template_store ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.terminology_provider ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.session_log ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.ehr ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.entry ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.entry_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.composition ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.composition_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.event_context ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';
ALTER TABLE ehr.event_context_history ADD namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002';


-- change unique constraint on template_store

ALTER TABLE ehr.template_store DROP CONSTRAINT template_store_template_id_key;
ALTER TABLE ehr.template_store DROP CONSTRAINT template_store_pkey, ADD PRIMARY KEY(id,template_id, namespace); 

