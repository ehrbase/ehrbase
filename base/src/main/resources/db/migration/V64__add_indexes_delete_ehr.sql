/*
 *  Copyright (c) 2021 Vitasystems GmbH.
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS attestation_has_audit_idx ON ehr.attestation(has_audit);
CREATE INDEX CONCURRENTLY IF NOT EXISTS attestation_reference_idx ON ehr.attestation(reference);

CREATE INDEX CONCURRENTLY IF NOT EXISTS attested_view_attestation_idx ON ehr.attested_view(attestation_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS compo_xref_child_idx ON ehr.compo_xref(child_uuid);

CREATE INDEX CONCURRENTLY IF NOT EXISTS composition_attestation_ref_idx ON ehr.composition(attestation_ref);
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS composition_has_audit_idx ON ehr.composition(has_audit);
CREATE INDEX CONCURRENTLY IF NOT EXISTS composition_in_contribution_idx ON ehr.composition(in_contribution);

CREATE INDEX CONCURRENTLY IF NOT EXISTS contribution_ehr_idx ON ehr.contribution(ehr_id);
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS contribution_has_audit_idx ON ehr.contribution(has_audit);

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS folder_has_audit_idx ON ehr.folder(has_audit);

CREATE INDEX CONCURRENTLY IF NOT EXISTS folder_items_folder_idx ON ehr.folder_items(folder_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS folder_items_in_contribution_idx ON ehr.folder_items(in_contribution);
CREATE INDEX CONCURRENTLY IF NOT EXISTS folder_items_obj_ref_idx ON ehr.folder_items(object_ref_id, in_contribution);

CREATE INDEX CONCURRENTLY IF NOT EXISTS identifier_party_idx ON ehr.identifier(party);

CREATE INDEX CONCURRENTLY IF NOT EXISTS status_attestation_ref_idx ON ehr.status(attestation_ref);
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS status_has_audit_idx ON ehr.status(has_audit);
CREATE INDEX CONCURRENTLY IF NOT EXISTS status_in_contribution_idx ON ehr.status(in_contribution);

CREATE INDEX CONCURRENTLY IF NOT EXISTS audit_details_committer_idx ON ehr.audit_details(committer);
CREATE INDEX CONCURRENTLY IF NOT EXISTS status_history_party_idx ON ehr.status_history(party);
CREATE INDEX CONCURRENTLY IF NOT EXISTS participation_history_performer_idx ON ehr.participation_history(performer);
CREATE INDEX CONCURRENTLY IF NOT EXISTS participation_performer_idx ON ehr.participation(performer);
CREATE INDEX CONCURRENTLY IF NOT EXISTS participation_performer_idx ON ehr.participation(performer);
CREATE INDEX CONCURRENTLY IF NOT EXISTS composition_history_composer_idx ON ehr.composition_history(composer);
CREATE INDEX CONCURRENTLY IF NOT EXISTS context_history_facility_idx ON ehr.event_context_history(facility);

CREATE INDEX CONCURRENTLY IF NOT EXISTS context_history_facility_idx ON ehr.event_context_history(facility);