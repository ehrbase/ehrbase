#!/bin/sh
psql -U postgres -h localhost << END_OF_SCRIPT

\c ehrbase;

TRUNCATE ehr.attestation, ehr.attested_view, ehr.contribution, ehr.audit_details,
         ehr.composition, ehr.folder, ehr.folder_hierarchy, ehr.object_ref, ehr.folder_items,
         ehr.composition_history, ehr.contribution_history, ehr.event_context, ehr.entry,
         ehr.compo_xref, ehr.participation, ehr.concept, ehr.ehr, ehr.status,
         ehr.party_identified, ehr.identifier;

TRUNCATE ehr.audit_details_history;
TRUNCATE ehr.containment;
TRUNCATE ehr.entry_history;
TRUNCATE ehr.event_context_history;
TRUNCATE ehr.folder_hierarchy_history;
TRUNCATE ehr.folder_history;
TRUNCATE ehr.folder_items_history;
TRUNCATE ehr.object_ref_history;
TRUNCATE ehr.participation_history;
TRUNCATE ehr.status_history;
TRUNCATE ehr.template_store;
TRUNCATE ehr.stored_query;

END_OF_SCRIPT
