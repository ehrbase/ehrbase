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

create or replace function admin_delete_ehr_full(ehr_id_param uuid)
    returns TABLE(deleted boolean)
    language plpgsql
    security definer
    SET search_path = ehr, pg_temp
as
$$
BEGIN
    -- Disable versioning triggers
    ALTER TABLE ehr.composition
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_hierarchy
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_items
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.object_ref
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.status
        DISABLE TRIGGER versioning_trigger;

    RETURN QUERY WITH
                     -- Query IDs
                     select_composition_ids
                         AS (SELECT id FROM ehr.composition WHERE ehr_id = ehr_id_param),
                     select_contribution_ids
                         AS (SELECT id FROM ehr.contribution WHERE ehr_id = ehr_id_param),

                     -- Delete data

                     -- ON DELETE CASCADE:
                     --   * ehr.attested_view
                     --   * ehr.entry
                     --   * ehr.event_context
                     --   * ehr.folder_hierarchy
                     --   * ehr.folder_items
                     --   * ehr.object_ref
                     --   * ehr.participation

                     delete_compo_xref
                         AS (DELETE FROM ehr.compo_xref cx USING select_composition_ids sci WHERE cx.master_uuid = sci.id OR cx.child_uuid = sci.id),
                     delete_composition
                         AS (DELETE FROM ehr.composition WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),
                     delete_status
                         AS (DELETE FROM ehr.status WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),
                     select_attestation_ids AS (SELECT id
                                                FROM ehr.attestation
                                                WHERE reference IN
                                                      (SELECT attestation_ref FROM delete_composition)
                                                   OR reference IN (SELECT attestation_ref FROM delete_status)),
                     delete_attestation
                         AS (DELETE FROM ehr.attestation a USING select_attestation_ids sa WHERE a.id = sa.id RETURNING a.reference, a.has_audit),
                     delete_attestation_ref
                         AS (DELETE FROM ehr.attestation_ref ar USING delete_attestation da WHERE ar.ref = da.reference),
                     delete_folder_items
                         AS (DELETE FROM ehr.folder_items fi USING select_contribution_ids sci WHERE fi.in_contribution = sci.id),
                     delete_folder_hierarchy
                         AS (DELETE FROM ehr.folder_hierarchy fh USING select_contribution_ids sci WHERE fh.in_contribution = sci.id),
                     delete_folder
                         AS (DELETE FROM ehr.folder f USING select_contribution_ids sci WHERE f.in_contribution = sci.id RETURNING f.id, f.has_audit),
                     delete_contribution
                         AS (DELETE FROM ehr.contribution c WHERE c.ehr_id = ehr_id_param RETURNING c.id, c.has_audit),
                     delete_ehr
                         AS (DELETE FROM ehr.ehr e WHERE e.id = ehr_id_param RETURNING e.access),
                     delete_access
                         AS (DELETE FROM ehr.access a USING delete_ehr de WHERE a.id = de.access),

                     -- Delete _history
                     delete_composition_history
                         AS (DELETE FROM ehr.composition_history WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),
                     delete_entry_history
                         AS (DELETE FROM ehr.entry_history eh USING delete_composition_history dch WHERE eh.composition_id = dch.id),
                     delete_event_context_hisotry
                         AS (DELETE FROM ehr.event_context_history ech USING delete_composition_history dch WHERE ech.composition_id = dch.id RETURNING ech.id),
                     delete_folder_history
                         AS (DELETE FROM ehr.folder_history fh USING select_contribution_ids sc WHERE fh.in_contribution = sc.id RETURNING fh.id, fh.has_audit),
                     delete_folder_items_history
                         AS (DELETE FROM ehr.folder_items_history fih USING select_contribution_ids sc WHERE fih.in_contribution = sc.id),
                     delete_folder_hierarchy_history
                         AS (DELETE FROM ehr.folder_hierarchy_history fhh USING select_contribution_ids sc WHERE fhh.in_contribution = sc.id),
                     delete_participation_history
                         AS (DELETE FROM ehr.participation_history ph USING delete_event_context_hisotry dech WHERE ph.event_context = dech.id),
                     object_ref_history
                         AS (DELETE FROM ehr.object_ref_history orh USING select_contribution_ids sc WHERE orh.in_contribution = sc.id),
                     delete_status_history
                         AS (DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),

                     -- Delete audit_details
                     delete_composition_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_composition dc WHERE ad.id = dc.has_audit),
                     delete_status_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_status ds WHERE ad.id = ds.has_audit),
                     delete_attestation_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_attestation da WHERE ad.id = da.has_audit),
                     delete_folder_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_folder df WHERE ad.id = df.has_audit),
                     delete_contribution_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_contribution dc WHERE ad.id = dc.has_audit),
                     delete_composition_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_composition_history dch WHERE ad.id = dch.has_audit),
                     delete_status_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_status_history dsh WHERE ad.id = dsh.has_audit),
                     delete_folder_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_folder_history dfh WHERE ad.id = dfh.has_audit)

                 SELECT true;

    -- Restore versioning triggers
    ALTER TABLE ehr.composition
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_hierarchy
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_items
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.object_ref
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.status
        ENABLE TRIGGER versioning_trigger;
END
$$;


create or replace function admin_delete_event_context_for_compo(compo_id_input uuid)
    returns TABLE(num integer, party uuid)
    strict
    language plpgsql
    security definer
    SET search_path = ehr, pg_temp
as
$$
DECLARE
    results RECORD;
BEGIN
    -- since for this admin op, we don't want to generate a history record for each delete!
    ALTER TABLE ehr.event_context DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation DISABLE TRIGGER versioning_trigger;

    RETURN QUERY WITH
                     linked_events(id) AS ( -- get linked EVENT_CONTEXT entities -- 0..1
                         SELECT id, facility FROM ehr.event_context WHERE composition_id = compo_id_input
                     ),
                     linked_event_history(id) AS ( -- get linked EVENT_CONTEXT entities -- 0..1
                         SELECT id, facility FROM ehr.event_context_history WHERE composition_id = compo_id_input
                     ),
                     linked_participations_for_events(id) AS ( -- get linked EVENT_CONTEXT entities -- for 0..1 events, each with * participations
                         SELECT id, performer FROM ehr.participation WHERE event_context IN (SELECT linked_events.id  FROM linked_events)
                     ),
                     linked_participations_for_events_history(id) AS ( -- get linked EVENT_CONTEXT entities -- for 0..1 events, each with * participations
                         SELECT id, performer FROM ehr.participation_history WHERE event_context IN (SELECT linked_event_history.id  FROM linked_event_history)
                     ),
                     parties(id) AS (
                         SELECT facility FROM linked_events
                         UNION
                         SELECT performer FROM linked_participations_for_events
                     ),
                     delete_participation AS (
                         DELETE FROM ehr.participation WHERE ehr.participation.id IN (SELECT linked_participations_for_events.id  FROM linked_participations_for_events)
                     ),
                     delete_participation_history AS (
                         DELETE FROM ehr.participation_history WHERE ehr.participation_history.id IN (SELECT linked_participations_for_events_history.id  FROM linked_participations_for_events_history)
                     ),
                     delete_event_contexts AS (
                         DELETE FROM ehr.event_context WHERE ehr.event_context.id IN (SELECT linked_events.id  FROM linked_events)
                     ),
                     delete_event_contexts_history AS (
                         DELETE FROM ehr.event_context_history WHERE ehr.event_context_history.id IN (SELECT linked_event_history.id  FROM linked_event_history)
                     )
                 SELECT 1, parties.id FROM parties;

    -- logging:

    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT b.id  FROM (
                              SELECT id, performer FROM ehr.participation
                              WHERE event_context IN (SELECT a.id  FROM (
                                                                            SELECT id, facility FROM ehr.event_context WHERE composition_id = compo_id_input
                                                                        ) AS a )
                          ) AS b
    )
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'PARTICIPATION', results.id, now();
        END LOOP;

    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT id, facility
        FROM ehr.event_context
        WHERE composition_id = compo_id_input)
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'EVENT_CONTEXT', results.id, now();
        END LOOP;

    -- restore disabled triggers
    ALTER TABLE ehr.event_context ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation ENABLE TRIGGER versioning_trigger;

END;
$$;





