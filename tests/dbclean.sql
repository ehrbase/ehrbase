-- dummy

TRUNCATE ehr.attestation, ehr.attested_view, ehr.contribution, ehr.audit_details,
         ehr.composition, ehr.folder, ehr.folder_hierarchy, ehr.object_ref, ehr.folder_items,
         ehr.composition_history, ehr.contribution_history, ehr.event_context, ehr.entry,
         ehr.compo_xref, ehr.participation, ehr.concept, ehr.ehr, ehr.status,
         ehr.party_identified, ehr.identifier CASCADE;

TRUNCATE ehr.audit_details_history CASCADE;
TRUNCATE ehr.containment CASCADE;
TRUNCATE ehr.entry_history CASCADE;
TRUNCATE ehr.event_context_history CASCADE;
TRUNCATE ehr.folder_hierarchy_history CASCADE;
TRUNCATE ehr.folder_history CASCADE;
TRUNCATE ehr.folder_items_history CASCADE;
TRUNCATE ehr.object_ref_history CASCADE;
TRUNCATE ehr.participation_history CASCADE;
TRUNCATE ehr.status_history CASCADE;
TRUNCATE ehr.template_store CASCADE;
