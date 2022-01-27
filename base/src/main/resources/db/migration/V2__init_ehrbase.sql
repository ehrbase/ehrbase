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

-- Generate EtherCIS tables for PostgreSQL 9.3
-- Author: Christian Chevalley
--
--
--
--    alter table com.ethercis.ehr.consult_req_attachement
--        drop constraint FKC199A3AAB95913AB;
--
--    alter table com.ethercis.ehr.consult_req_attachement
--        drop constraint FKC199A3AA4204581F;
--

-- 20170605 RVE:
-- this file is a copy of jooq-pg/src/main/resources/ddls/pgsql_ehr.ddl with the following
-- modififactions:
--   - places extensions in the ext schema due to flyway restrictions
--   - replaced all VARCHAR with TEXT (because our tzid is longer than what fits)
--
-- 20220126 CCH: refactored all migrations to be in one, removed unnecessary objects

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- EHRbase Types

CREATE TYPE ehr.code_phrase AS
(
    terminology_id_value text,
    code_string          text
);
ALTER TYPE ehr.code_phrase OWNER TO ehrbase;

CREATE TYPE ehr.contribution_change_type AS ENUM (
    'creation',
    'amendment',
    'modification',
    'synthesis',
    'Unknown',
    'deleted'
    );
ALTER TYPE ehr.contribution_change_type OWNER TO ehrbase;

CREATE TYPE ehr.contribution_data_type AS ENUM (
    'composition',
    'folder',
    'ehr',
    'system',
    'other'
    );
ALTER TYPE ehr.contribution_data_type OWNER TO ehrbase;

CREATE TYPE ehr.contribution_state AS ENUM (
    'complete',
    'incomplete',
    'deleted'
    );
ALTER TYPE ehr.contribution_state OWNER TO ehrbase;

CREATE TYPE ehr.dv_coded_text AS
(
    value         text,
    defining_code ehr.code_phrase,
    formatting    text,
    language      ehr.code_phrase,
    encoding      ehr.code_phrase,
    term_mapping  text[]
);
ALTER TYPE ehr.dv_coded_text OWNER TO ehrbase;

CREATE TYPE ehr.entry_type AS ENUM (
    'section',
    'care_entry',
    'admin',
    'proxy'
    );
ALTER TYPE ehr.entry_type OWNER TO ehrbase;

CREATE TYPE ehr.party_ref_id_type AS ENUM (
    'generic_id',
    'object_version_id',
    'hier_object_id',
    'undefined'
    );
ALTER TYPE ehr.party_ref_id_type OWNER TO ehrbase;

CREATE TYPE ehr.party_type AS ENUM (
    'party_identified',
    'party_self',
    'party_related'
    );
ALTER TYPE ehr.party_type OWNER TO ehrbase;


-- EHRbase Functions

CREATE FUNCTION ehr.admin_delete_all_templates() RETURNS integer
    LANGUAGE plpgsql
AS
$$
DECLARE
    deleted integer;
BEGIN
    SELECT count(*) INTO deleted FROM ehr.template_store;
    DELETE FROM ehr.template_store ts WHERE ts.id NOTNULL;
    RETURN deleted;
END;
$$;
ALTER FUNCTION ehr.admin_delete_all_templates() OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_attestation(attest_ref_input uuid)
    RETURNS TABLE
            (
                audit uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
DECLARE
    results RECORD;
BEGIN
    RETURN QUERY WITH
                     -- extract info about referenced audit
                     linked_audit(id) AS (
                         SELECT ehr.attestation.has_audit
                         FROM ehr.attestation
                         WHERE reference = attest_ref_input
                     ),
                     -- extract info about attestation linked by the given reference
                     linked_attestation(id) AS (
                         SELECT ehr.attestation.id
                         FROM ehr.attestation
                         WHERE reference = attest_ref_input
                     ),
                     -- extract info about attested_view linked by the extracted attestations
                     linked_attested_view(id) AS (
                         SELECT ehr.attested_view.id
                         FROM ehr.attested_view
                         WHERE attestation_id IN (SELECT linked_attestation.id FROM linked_attestation)
                     ),
                     -- delete attested_view
                     delete_attested_view AS (
                         DELETE FROM ehr.attested_view WHERE id IN (SELECT linked_attested_view.id FROM linked_attested_view)
                     ),
                     -- delete attestation
                     delete_attestation AS (
                         DELETE FROM ehr.attestation WHERE id IN (SELECT linked_attestation.id FROM linked_attestation)
                     ),
                     -- delete attestation_ref
                     delete_attestation_ref AS (
                         DELETE FROM ehr.attestation_ref WHERE id = attest_ref_input
                     )

                 SELECT linked_audit.id
                 FROM linked_audit;

    -- logging:

    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT ehr.attested_view.id
        FROM ehr.attested_view
        WHERE attestation_id IN (
            SELECT a.id
            FROM (
                     SELECT ehr.attestation.id
                     FROM ehr.attestation
                     WHERE reference = attest_ref_input)
                     AS a
        )
    )
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ATTESTED_VIEW', results.id, now();
        END LOOP;

    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT ehr.attestation.id
        FROM ehr.attestation
        WHERE reference = attest_ref_input)
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ATTESTATION', results.id, now();
        END LOOP;

    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ATTESTATION_REF', attest_ref_input, now();

END;
$$;
ALTER FUNCTION ehr.admin_delete_attestation(attest_ref_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_audit(audit_input uuid)
    RETURNS TABLE
            (
                num   integer,
                party uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH
                     -- extract info about referenced system, before deleting audit
                     scope_system(system_id) AS ( -- get current scope's system ID
                         SELECT ehr.audit_details.system_id
                         FROM ehr.audit_details
                         WHERE id = audit_input
                         GROUP BY ehr.audit_details.system_id
                     ),
                     -- extract info about referenced audits, before deleting audit
                     systems_audits(system_id, audit_id) AS ( -- get table of audits and their system ID
                         SELECT ehr.system.id AS system_id, ehr.audit_details.id AS audit_id
                         FROM ehr.audit_details,
                              ehr.system
                         WHERE ehr.system.id = ehr.audit_details.system_id
                     ),
                     linked_party(id) AS ( -- remember linked party before deletion
                         SELECT ehr.audit_details.committer FROM ehr.audit_details WHERE id = audit_input
                     ),
                     delete_audit_details AS (
                         DELETE FROM ehr.audit_details WHERE id = audit_input
                     )

                 SELECT 1, linked_party.id
                 FROM linked_party;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'AUDIT_DETAILS', audit_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_audit(audit_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_composition(compo_id_input uuid)
    RETURNS TABLE
            (
                num          integer,
                contribution uuid,
                party        uuid,
                audit        uuid,
                attestation  uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
DECLARE
    results RECORD;
BEGIN
    RETURN QUERY WITH linked_entries(id) AS ( -- get linked ENTRY entities
        SELECT id
        FROM ehr.entry
        WHERE composition_id = compo_id_input
    ),
                      linked_misc(contrib, party, audit, attestation) AS (
                          SELECT in_contribution, composer, has_audit, attestation_ref
                          FROM ehr.composition
                          WHERE id = compo_id_input
                      ),
                      delete_entries AS (
                          DELETE FROM ehr.entry WHERE ehr.entry.id IN (SELECT linked_entries.id FROM linked_entries)
                      ),
                      -- delete composition itself
                      delete_composition AS (
                          DELETE FROM ehr.composition WHERE id = compo_id_input
                      )
                 SELECT 1, linked_misc.contrib, linked_misc.party, linked_misc.audit, linked_misc.attestation
                 FROM linked_misc;

    -- logging:

    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT a.id
        FROM (
                 SELECT id
                 FROM ehr.entry
                 WHERE composition_id = compo_id_input
             ) AS a)
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'ENTRY', results.id, now();
        END LOOP;

    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'COMPOSITION', compo_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_composition(compo_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_composition_history(compo_input uuid)
    RETURNS TABLE
            (
                num integer
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH delete_entry_history AS (
        DELETE FROM ehr.entry_history WHERE composition_id = compo_input
    ),
                      delete_composition_history AS (
                          DELETE FROM ehr.composition_history WHERE id = compo_input
                      )

                 SELECT 1;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - Linked to COMPOSITION ID: % - Time: %', 'entry_history', compo_input, now();
    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'COMPOSITION_HISTORY', compo_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_composition_history(compo_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_contribution(contrib_id_input uuid)
    RETURNS TABLE
            (
                num   integer,
                audit uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH linked_misc(audit) AS (
        SELECT has_audit
        FROM ehr.contribution
        WHERE id = contrib_id_input
    ),
                      -- delete contribution itself
                      delete_composition AS (
                          DELETE FROM ehr.contribution WHERE id = contrib_id_input
                      )
                 SELECT 1, linked_misc.audit
                 FROM linked_misc;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'CONTRIBUTION', contrib_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_contribution(contrib_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_ehr(ehr_id_input uuid)
    RETURNS TABLE
            (
                num          integer,
                status_audit uuid,
                status_party uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH linked_status(has_audit) AS ( -- get linked STATUS parameters
        SELECT has_audit
        FROM ehr.status AS s
        WHERE ehr_id = ehr_id_input
        UNION
        SELECT has_audit
        FROM ehr.status_history AS sh
        WHERE ehr_id = ehr_id_input
    ),
                      -- delete the EHR itself
                      delete_ehr AS (
                          DELETE FROM ehr.ehr WHERE id = ehr_id_input
                      ),
                      linked_party(id) AS ( -- formally always one
                          SELECT party
                          FROM ehr.status
                          WHERE ehr_id = ehr_id_input
                      ),
                      -- Note: not handling the system referenced by EHR, because there is always at least one audit referencing it, too. See separated audit handling.
                      -- delete status
                      delete_status AS (
                          DELETE FROM ehr.status WHERE ehr_id = ehr_id_input
                      )

                 SELECT 1, linked_status.has_audit, linked_party.id
                 FROM linked_status,
                      linked_party;

-- logging:
    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'EHR', ehr_id_input, now();
    RAISE NOTICE 'Admin deletion - Type: % - Linked to EHR ID: % - Time: %', 'STATUS', ehr_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_ehr(ehr_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_ehr_full(ehr_id_param uuid)
    RETURNS TABLE
            (
                deleted boolean
            )
    LANGUAGE plpgsql
AS
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
ALTER FUNCTION ehr.admin_delete_ehr_full(ehr_id_param uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_ehr_history(ehr_id_input uuid)
    RETURNS TABLE
            (
                num integer
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH
                     -- delete status_history
                     delete_status_history AS (
                         DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_input
                     )

                 SELECT 1;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - Linked to EHR ID: % - Time: %', 'STATUS_HISTORY', ehr_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_ehr_history(ehr_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_event_context_for_compo(compo_id_input uuid)
    RETURNS TABLE
            (
                num   integer,
                party uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
DECLARE
    results RECORD;
BEGIN
    -- since for this admin op, we don't want to generate a history record for each delete!
    ALTER TABLE ehr.event_context
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        DISABLE TRIGGER versioning_trigger;

    RETURN QUERY WITH linked_events(id) AS ( -- get linked EVENT_CONTEXT entities -- 0..1
        SELECT id, facility FROM ehr.event_context WHERE composition_id = compo_id_input
    ),
                      linked_event_history(id) AS ( -- get linked EVENT_CONTEXT entities -- 0..1
                          SELECT id, facility FROM ehr.event_context_history WHERE composition_id = compo_id_input
                      ),
                      linked_participations_for_events(id)
                          AS ( -- get linked EVENT_CONTEXT entities -- for 0..1 events, each with * participations
                          SELECT id, performer
                          FROM ehr.participation
                          WHERE event_context IN (SELECT linked_events.id FROM linked_events)
                      ),
                      linked_participations_for_events_history(id)
                          AS ( -- get linked EVENT_CONTEXT entities -- for 0..1 events, each with * participations
                          SELECT id, performer
                          FROM ehr.participation_history
                          WHERE event_context IN (SELECT linked_event_history.id FROM linked_event_history)
                      ),
                      parties(id) AS (
                          SELECT facility
                          FROM linked_events
                          UNION
                          SELECT performer
                          FROM linked_participations_for_events
                      ),
                      delete_participation AS (
                          DELETE FROM ehr.participation WHERE ehr.participation.id IN
                                                              (SELECT linked_participations_for_events.id
                                                               FROM linked_participations_for_events)
                      ),
                      delete_participation_history AS (
                          DELETE FROM ehr.participation_history WHERE ehr.participation_history.id IN
                                                                      (SELECT linked_participations_for_events_history.id
                                                                       FROM linked_participations_for_events_history)
                      ),
                      delete_event_contexts AS (
                          DELETE FROM ehr.event_context WHERE ehr.event_context.id IN (SELECT linked_events.id FROM linked_events)
                      ),
                      delete_event_contexts_history AS (
                          DELETE FROM ehr.event_context_history WHERE ehr.event_context_history.id IN
                                                                      (SELECT linked_event_history.id FROM linked_event_history)
                      )
                 SELECT 1, parties.id
                 FROM parties;

    -- logging:

    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT b.id
        FROM (
                 SELECT id, performer
                 FROM ehr.participation
                 WHERE event_context IN (SELECT a.id
                                         FROM (
                                                  SELECT id, facility
                                                  FROM ehr.event_context
                                                  WHERE composition_id = compo_id_input
                                              ) AS a)
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
    ALTER TABLE ehr.event_context
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        ENABLE TRIGGER versioning_trigger;

END;
$$;
ALTER FUNCTION ehr.admin_delete_event_context_for_compo(compo_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_folder(folder_id_input uuid)
    RETURNS TABLE
            (
                contribution uuid,
                child        uuid,
                audit        uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
DECLARE
    results RECORD;
BEGIN
    RETURN QUERY WITH
                     -- order to delete things:
                     -- all folders (scope's parent + children) itself from FOLDER, order shouldn't matter
                     -- all their FOLDER_HIERARCHY entries
                     -- all FOLDER_ITEMS matching FOLDER.IDs
                     -- all OBJECT_REF mentioned in FOLDER_ITEMS
                     -- all CONTRIBUTIONs (1..*) collected along the way above
                     -- all audits
                     -- AFTERWARDS and separate: deletion of all matching *_HISTORY table entries

                     -- recursively retrieve all layers of children
                     RECURSIVE
                     linked_children AS (
                         SELECT child_folder, in_contribution
                         FROM ehr.folder_hierarchy
                         WHERE parent_folder = folder_id_input
                         UNION
                         SELECT fh.child_folder, fh.in_contribution
                         FROM ehr.folder_hierarchy fh
                                  INNER JOIN linked_children lc ON lc.child_folder = fh.parent_folder
                     ),
                     linked_object_ref AS (
                         SELECT DISTINCT object_ref_id
                         FROM ehr.folder_items
                         WHERE (folder_id = folder_id_input)
                            OR (folder_id IN (SELECT linked_children.child_folder FROM linked_children))
                     ),
                     linked_contribution AS (
                         SELECT DISTINCT in_contribution
                         FROM ehr.folder
                         WHERE (id = folder_id_input)
                            OR (id IN (SELECT linked_children.child_folder FROM linked_children))

                         UNION

                         SELECT DISTINCT in_contribution
                         FROM ehr.folder_items
                         WHERE (folder_id = folder_id_input)
                            OR (folder_id IN (SELECT linked_children.child_folder FROM linked_children))

                         UNION

                         SELECT DISTINCT in_contribution
                         FROM ehr.object_ref
                         WHERE id IN (SELECT linked_object_ref.object_ref_id FROM linked_object_ref)

                         UNION

                         SELECT DISTINCT in_contribution
                         FROM linked_children
                     ),
                     linked_audit AS (
                         SELECT DISTINCT has_audit
                         FROM ehr.folder
                         WHERE (id = folder_id_input)
                            OR (id IN (SELECT linked_children.child_folder FROM linked_children))
                     ),
                     remove_directory AS (
                         UPDATE ehr.ehr -- remove link to ehr and then actually delete the folder
                             SET directory = NULL
                             WHERE directory = folder_id_input
                     ),
                     delete_folders AS (
                         DELETE FROM ehr.folder WHERE (id = folder_id_input) OR
                                                      (id IN (SELECT linked_children.child_folder FROM linked_children))
                     ),
                     delete_hierarchy AS (
                         DELETE FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
                     ),
                     delete_items AS (
                         DELETE FROM ehr.folder_items WHERE (folder_id = folder_id_input) OR
                                                            (folder_id IN (SELECT linked_children.child_folder FROM linked_children))
                     ),
                     delete_object_ref AS (
                         DELETE FROM ehr.object_ref WHERE id IN (SELECT linked_object_ref.object_ref_id FROM linked_object_ref)
                     )
                     -- returning contribution IDs to delete separate
                     -- same with children IDs, as *_HISTORY tables of ID sets ((original input folder + children), and obj_ref via their contribs) needs to be deleted separate, too.
                     -- as well as audits
                 SELECT DISTINCT linked_contribution.in_contribution,
                                 linked_children.child_folder,
                                 linked_audit.has_audit
                 FROM linked_contribution,
                      linked_children,
                      linked_audit;

    -- logging:

    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'FOLDER', folder_id_input, now();
    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT a.child_folder
        FROM (
                 SELECT child_folder, in_contribution FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
             ) AS a)
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'FOLDER', results.child_folder, now();
        END LOOP;

    RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_HIERARCHY', folder_id_input, now();

    RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_ITEMS', folder_id_input, now();
    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT a.child_folder
        FROM (
                 SELECT child_folder, in_contribution FROM ehr.folder_hierarchy WHERE parent_folder = folder_id_input
             ) AS a)
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_ITEMS', results.child_folder, now();
        END LOOP;

    -- looping query is reconstructed from above CTEs, because they can't be reused here
    FOR results IN (
        SELECT a.object_ref_id
        FROM (
                 SELECT DISTINCT object_ref_id
                 FROM ehr.folder_items
                 WHERE (folder_id = folder_id_input)
                    OR (folder_id IN (SELECT b.child_folder
                                      FROM (
                                               SELECT child_folder, in_contribution
                                               FROM ehr.folder_hierarchy
                                               WHERE parent_folder = folder_id_input
                                           ) AS b))
             ) AS a
    )
        LOOP
            RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'OBJECT_REF', results.object_ref_id, now();
        END LOOP;
END;
$$;
ALTER FUNCTION ehr.admin_delete_folder(folder_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_folder_history(folder_id_input uuid)
    RETURNS TABLE
            (
                num integer
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH delete_folders AS (
        DELETE FROM ehr.folder_history WHERE id = folder_id_input
    ),
                      delete_hierarchy AS (
                          DELETE FROM ehr.folder_hierarchy_history WHERE parent_folder = folder_id_input
                      ),
                      delete_items AS (
                          DELETE FROM ehr.folder_items_history WHERE folder_id = folder_id_input
                      )

                 SELECT 1;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_HISTORY', folder_id_input, now();
    RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_HIERARCHY_HISTORY', folder_id_input, now();
    RAISE NOTICE 'Admin deletion - Type: % - Linked to FOLDER ID: % - Time: %', 'FOLDER_ITEMS_HISTORY', folder_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_folder_history(folder_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_folder_obj_ref_history(contribution_id_input uuid)
    RETURNS TABLE
            (
                num integer
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH delete_object_ref AS (
        DELETE FROM ehr.object_ref_history WHERE in_contribution = contribution_id_input
    )

                 SELECT 1;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - Linked to CONTRIBUTION ID: % - Time: %', 'OBJECT_REF_HISTORY', contribution_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_folder_obj_ref_history(contribution_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_status(status_id_input uuid)
    RETURNS TABLE
            (
                num          integer,
                status_audit uuid,
                status_party uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH linked_misc(has_audit, party) AS ( -- formally always one
        SELECT has_audit, party
        FROM ehr.status
        WHERE id = status_id_input
    ),
                      -- delete status
                      delete_status AS (
                          DELETE FROM ehr.status WHERE id = status_id_input
                      )

                 SELECT 1, linked_misc.has_audit, linked_misc.party
                 FROM linked_misc;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - ID: % - Time: %', 'STATUS', status_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_status(status_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_status_history(status_id_input uuid)
    RETURNS TABLE
            (
                num integer
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH
                     -- delete status_history
                     delete_status_history AS (
                         DELETE FROM ehr.status_history WHERE id = status_id_input
                     )

                 SELECT 1;

    -- logging:
    RAISE NOTICE 'Admin deletion - Type: % - Linked to STATUS ID: % - Time: %', 'STATUS_HISTORY', status_id_input, now();
END;
$$;
ALTER FUNCTION ehr.admin_delete_status_history(status_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_delete_template(target_id text) RETURNS integer
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE FROM ehr.template_store ts WHERE ts.template_id = target_id;
    RETURN 1;
END;
$$;
ALTER FUNCTION ehr.admin_delete_template(target_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_get_linked_compositions(ehr_id_input uuid)
    RETURNS TABLE
            (
                composition uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH linked_compo(id) AS ( -- get linked CONTRIBUTION parameters
        SELECT id
        FROM ehr.composition
        WHERE ehr_id = ehr_id_input
    )

                 SELECT *
                 FROM linked_compo;
END;
$$;
ALTER FUNCTION ehr.admin_get_linked_compositions(ehr_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_get_linked_compositions_for_contrib(contrib_id_input uuid)
    RETURNS TABLE
            (
                composition uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH linked_compo(id) AS ( -- get linked CONTRIBUTION parameters
        SELECT id
        FROM ehr.composition
        WHERE in_contribution = contrib_id_input
    )

                 SELECT *
                 FROM linked_compo;
END;
$$;
ALTER FUNCTION ehr.admin_get_linked_compositions_for_contrib(contrib_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_get_linked_contributions(ehr_id_input uuid)
    RETURNS TABLE
            (
                contribution uuid,
                audit        uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH linked_contrib(id, audit) AS ( -- get linked CONTRIBUTION parameters
        SELECT id, has_audit
        FROM ehr.contribution
        WHERE ehr_id = ehr_id_input
    )

                 SELECT *
                 FROM linked_contrib;
END;
$$;
ALTER FUNCTION ehr.admin_get_linked_contributions(ehr_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_get_linked_status_for_contrib(contrib_id_input uuid)
    RETURNS TABLE
            (
                status uuid
            )
    LANGUAGE plpgsql
    STRICT
AS
$$
BEGIN
    RETURN QUERY WITH linked_status(id) AS ( -- get linked CONTRIBUTION parameters
        SELECT id
        FROM ehr.status
        WHERE in_contribution = contrib_id_input
    )

                 SELECT *
                 FROM linked_status;
END;
$$;
ALTER FUNCTION ehr.admin_get_linked_status_for_contrib(contrib_id_input uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_get_template_usage(target_id text)
    RETURNS TABLE
            (
                composition_id uuid
            )
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN query
        SELECT e.composition_id
        FROM ehr.entry e
        WHERE e.template_id = target_id
        UNION
        (
            SELECT eh.composition_id
            FROM ehr.entry_history eh
            WHERE eh.template_id = target_id
        );
END;
$$;
ALTER FUNCTION ehr.admin_get_template_usage(target_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.admin_update_template(target_id text, update_content text) RETURNS text
    LANGUAGE plpgsql
AS
$$
DECLARE
    new_template TEXT;
BEGIN
    UPDATE ehr.template_store
    SET "content" = update_content
    WHERE template_id = target_id;
    SELECT ts."content"
    INTO new_template
    FROM ehr.template_store ts
    WHERE ts.template_id = target_id;
    RETURN new_template;
END;
$$;
ALTER FUNCTION ehr.admin_update_template(target_id text, update_content text) OWNER TO ehrbase;

--
-- TOC entry 344 (class 1255 OID 39928)
-- Name: aql_node_name_predicate(jsonb, text, text); Type: FUNCTION; Schema: ehr; Owner: ehrbase
--

CREATE FUNCTION ehr.aql_node_name_predicate(entry jsonb, name_value_predicate text, jsonb_path text) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
DECLARE
    entry_segment           JSONB;
    jsquery_node_expression TEXT;
    subnode                 JSONB;
BEGIN

    -- get the segment for the predicate

    SELECT jsonb_extract_path(entry, VARIADIC string_to_array(jsonb_path, ',')) INTO STRICT entry_segment;

    IF (entry_segment IS NULL) THEN
        RETURN NULL ;
    END IF;

    -- identify structure with name/value matching argument
    IF (jsonb_typeof(entry_segment) <> 'array') THEN
        IF ((entry_segment #>> '{/name,0,value}') = name_value_predicate) THEN
            RETURN entry_segment;
        ELSE
            RETURN NULL;
        END IF;
    END IF;

    FOR subnode IN SELECT jsonb_array_elements(entry_segment)
        LOOP
            IF ((subnode #>> '{/name,0,value}') = name_value_predicate) THEN
                RETURN subnode;
            END IF;
        END LOOP;

    RETURN NULL;

END
$$;
ALTER FUNCTION ehr.aql_node_name_predicate(entry jsonb, name_value_predicate text, jsonb_path text) OWNER TO ehrbase;

CREATE FUNCTION ehr.camel_to_snake(literal text) RETURNS text
    LANGUAGE plpgsql
AS
$$
DECLARE
    out_literal  TEXT := '';
    literal_size INT;
    char_at      TEXT;
    ndx          INT;
BEGIN
    literal_size := length(literal);
    if (literal_size = 0) then
        return literal;
    end if;
    ndx = 1;
    while ndx <= literal_size
        loop
            char_at := substr(literal, ndx, 1);
            if (char_at ~ '[A-Z]') then
                if (ndx > 1 AND substr(literal, ndx - 1, 1) <> '<') then
                    out_literal = out_literal || '_';
                end if;
                out_literal = out_literal || lower(char_at);
            else
                out_literal = out_literal || char_at;
            end if;
            ndx := ndx + 1;
        end loop;
    out_literal := replace(replace(replace(out_literal, 'u_r_i', 'uri'), 'i_d', 'id'), 'i_s_m', 'ism');
    return out_literal;
END
$$;
ALTER FUNCTION ehr.camel_to_snake(literal text) OWNER TO ehrbase;

CREATE FUNCTION ehr.composition_name(content jsonb) RETURNS text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        (with root_json as (
            select jsonb_object_keys(content) root)
         select trim(LEADING '''' FROM
                     (trim(TRAILING ''']' FROM (regexp_split_to_array(root_json.root, 'and name/value='))[2])))
         from root_json
         where root like '/composition%');
END
$$;
ALTER FUNCTION ehr.composition_name(content jsonb) OWNER TO ehrbase;

CREATE FUNCTION ehr.composition_uid(composition_uid uuid, server_id text) RETURNS text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (select "composition"."id" || '::' || server_id || '::' || 1
        + COALESCE(
                                                                              (select count(*)
                                                                               from "ehr"."composition_history"
                                                                               where "composition_history"."id" = composition_uid)
                                                                          , 0)
            from ehr.composition
            where composition.id = composition_uid);
END
$$;
ALTER FUNCTION ehr.composition_uid(composition_uid uuid, server_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.delete_orphan_history() RETURNS boolean
    LANGUAGE sql
AS
$$
WITH delete_orphan_compo_history as (
    delete from ehr.composition_history where not exists(select 1 from ehr.composition where id = ehr.composition_history.id)
),
     delete_orphan_event_context_history as (
         delete from ehr.event_context_history where not exists(select 1
                                                                from ehr.event_context
                                                                where event_context.composition_id =
                                                                      ehr.event_context_history.composition_id)
     ),
     delete_orphan_participation_history as (
         delete from ehr.participation_history where not exists(select 1
                                                                from ehr.participation
                                                                where participation.event_context = ehr.participation_history.event_context)
     ),
     delete_orphan_entry_history as (
         delete from ehr.entry_history where not exists(select 1
                                                        from ehr.composition
                                                        where composition.id = ehr.entry_history.composition_id)
     ),
     delete_orphan_party_identified as (
         DELETE FROM ehr.party_identified WHERE ehr.party_usage(party_identified.id) = 0
     )
select true;
$$;
ALTER FUNCTION ehr.delete_orphan_history() OWNER TO ehrbase;

CREATE FUNCTION ehr.ehr_status_uid(ehr_uuid uuid, server_id text) RETURNS text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (select "status"."id" || '::' || server_id || '::' || 1
        + COALESCE(
                                                                         (select count(*)
                                                                          from "ehr"."status_history"
                                                                          where "status_history"."ehr_id" = ehr_uuid
                                                                          group by "ehr"."status_history"."ehr_id")
                                                                     , 0)
            from ehr.status
            where status.ehr_id = ehr_uuid);
END
$$;
ALTER FUNCTION ehr.ehr_status_uid(ehr_uuid uuid, server_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.folder_uid(folder_uid uuid, server_id text) RETURNS text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        select "folder_join"."id" || '::' || server_id || '::' || 1
            + COALESCE(
                                                                          (select count(*)
                                                                           from "ehr"."folder_history"
                                                                           where folder_uid = "ehr"."folder_history"."id"
                                                                           group by "ehr"."folder_history"."id")
                                                                      , 0) as "uid/value"
        from "ehr"."entry"
                 right outer join "ehr"."folder" as "folder_join"
                                  on "folder_join"."id" = folder_uid
        limit 1
    );
END
$$;
ALTER FUNCTION ehr.folder_uid(folder_uid uuid, server_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.get_system_version() RETURNS text
    LANGUAGE plpgsql
AS
$$
DECLARE
    version_string TEXT;
BEGIN
    SELECT VERSION() INTO version_string;
    RETURN version_string;
END;
$$;
ALTER FUNCTION ehr.get_system_version() OWNER TO ehrbase;

CREATE FUNCTION ehr.iso_timestamp(timestamp with time zone) RETURNS character varying
    LANGUAGE sql
    IMMUTABLE
AS
$_$
select substring(xmlelement(name x, $1)::varchar from 4 for 19)
$_$;
ALTER FUNCTION ehr.iso_timestamp(timestamp with time zone) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_archetype_details(archetype_node_id text, template_id text) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', 'ARCHETYPED',
                        'archetype_id', jsonb_build_object(
                                '_type', 'ARCHETYPE_ID',
                                'value', archetype_node_id
                            ),
                        'template_id', jsonb_build_object(
                                '_type', 'TEMPLATE_ID',
                                'value', template_id
                            ),
                        'rm_version', '1.0.2'
                    )
            );
END
$$;
ALTER FUNCTION ehr.js_archetype_details(archetype_node_id text, template_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_archetype_details(archetype_node_id text, template_id text, rm_version text) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', 'ARCHETYPED',
                        'archetype_id', jsonb_build_object(
                                '_type', 'ARCHETYPE_ID',
                                'value', archetype_node_id
                            ),
                        'template_id', jsonb_build_object(
                                '_type', 'TEMPLATE_ID',
                                'value', template_id
                            ),
                        'rm_version', rm_version
                    )
            );
END
$$;
ALTER FUNCTION ehr.js_archetype_details(archetype_node_id text, template_id text, rm_version text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_archetyped(text, text) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    archetype_id ALIAS FOR $1;
    template_id ALIAS FOR $2;
BEGIN
    RETURN
        json_build_object(
                '_type', 'ARCHETYPED',
                'archetype_id',
                json_build_object(
                        '_type', 'ARCHETYPE_ID',
                        'value', archetype_id
                    ),
                template_id,
                json_build_object(
                        '_type', 'TEMPLATE_ID',
                        'value', template_id
                    ),
                'rm_version', '1.0.1'
            );
END
$_$;
ALTER FUNCTION ehr.js_archetyped(text, text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_audit_details(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    audit_details_uuid ALIAS FOR $1;
BEGIN
    RETURN (
        SELECT jsonb_strip_nulls(
                       jsonb_build_object(
                               '_type', 'AUDIT_DETAILS',
                               'system_id', ehr.js_canonical_hier_object_id(system.settings),
                               'time_committed',
                               ehr.js_dv_date_time(audit_details.time_committed, audit_details.time_committed_tzid),
                               'change_type', ehr.js_dv_coded_text_inner((audit_details.change_type,
                                                                          (('openehr',
                                                                            ehr.map_change_type_to_codestring(audit_details.change_type::TEXT))::ehr.code_phrase),
                                                                          NULL,
                                                                          NULL,
                                                                          NULL,
                                                                          NULL)::ehr.dv_coded_text),
                               'description', ehr.js_dv_text(audit_details.description),
                               'committer', ehr.js_canonical_party_identified(audit_details.committer)
                           )
                   )
        FROM ehr.audit_details
                 JOIN ehr.system ON system.id = audit_details.system_id
        WHERE audit_details.id = audit_details_uuid
    );
END
$_$;
ALTER FUNCTION ehr.js_audit_details(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_dv_quantity(magnitude double precision, units text, _precision integer,
                                             accuracy_percent boolean) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', 'DV_QUANTITY',
                        'magnitude', magnitude,
                        'units', units,
                        'precision', _precision,
                        'accuracy_is_percent', accuracy_percent
                    )
            );
END
$$;
ALTER FUNCTION ehr.js_canonical_dv_quantity(magnitude double precision, units text, _precision integer, accuracy_percent boolean) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_generic_id(scheme text, id_value text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', 'GENERIC_ID',
                        'value', id_value,
                        'scheme', scheme
                    )
            );
END
$$;
ALTER FUNCTION ehr.js_canonical_generic_id(scheme text, id_value text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_hier_object_id(id_value text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        json_build_object(
                '_type', 'HIER_OBJECT_ID',
                'value', id_value
            );
END
$$;
ALTER FUNCTION ehr.js_canonical_hier_object_id(id_value text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_hier_object_id(ehr_id uuid) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        json_build_object(
                '_type', 'HIER_OBJECT_ID',
                'value', ehr_id
            );
END
$$;
ALTER FUNCTION ehr.js_canonical_hier_object_id(ehr_id uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_object_id(objectid_type ehr.party_ref_id_type, scheme text, id_value text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        SELECT CASE
                   WHEN objectid_type = 'generic_id'
                       THEN
                       ehr.js_canonical_generic_id(scheme, id_value)
                   WHEN objectid_type = 'hier_object_id'
                       THEN
                       ehr.js_canonical_hier_object_id(id_value)
                   WHEN objectid_type = 'object_version_id'
                       THEN
                       ehr.js_canonical_object_version_id(id_value)
                   WHEN objectid_type = 'undefined'
                       THEN
                       NULL
                   END
    );
END
$$;
ALTER FUNCTION ehr.js_canonical_object_id(objectid_type ehr.party_ref_id_type, scheme text, id_value text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_object_version_id(id_value text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        json_build_object(
                '_type', 'OBJECT_VERSION_ID',
                'value', id_value
            );
END
$$;
ALTER FUNCTION ehr.js_canonical_object_version_id(id_value text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_participations(context_id uuid) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (SELECT jsonb_array_elements(jsonb_build_array(ehr.js_participations(context_id))));
END
$$;
ALTER FUNCTION ehr.js_canonical_participations(context_id uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_party_identified(refid uuid) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        WITH party_values AS (
            SELECT party_identified.name                as name,
                   party_identified.party_ref_value     as value,
                   party_identified.party_ref_scheme    as scheme,
                   party_identified.party_ref_namespace as namespace,
                   party_identified.party_ref_type      as ref_type,
                   party_identified.party_type          as party_type,
                   party_identified.relationship        as relationship,
                   party_identified.object_id_type      as objectid_type
            FROM ehr.party_identified
            WHERE party_identified.id = refid
        )
        SELECT CASE
                   WHEN party_values.party_type = 'party_identified'
                       THEN
                       ehr.json_party_identified(party_values.name, refid, party_values.namespace,
                                                 party_values.ref_type, party_values.scheme, party_values.value,
                                                 party_values.objectid_type)::json
                   WHEN party_values.party_type = 'party_self'
                       THEN
                       ehr.json_party_self(refid, party_values.namespace, party_values.ref_type, party_values.scheme,
                                           party_values.value, party_values.objectid_type)::json
                   WHEN party_values.party_type = 'party_related'
                       THEN
                       ehr.json_party_related(party_values.name, refid, party_values.namespace, party_values.ref_type,
                                              party_values.scheme, party_values.value, party_values.objectid_type,
                                              relationship)::json
                   END
        FROM party_values
    );
END
$$;
ALTER FUNCTION ehr.js_canonical_party_identified(refid uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_canonical_party_ref(namespace text, type text, scheme text, id text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', 'PARTY_REF',
                        'namespace', namespace,
                        'type', type,
                        'id', ehr.js_canonical_generic_id(scheme, id)
                    )
            );
END
$$;
ALTER FUNCTION ehr.js_canonical_party_ref(namespace text, type text, scheme text, id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_code_phrase(codephrase ehr.code_phrase) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        json_build_object(
                '_type', 'CODE_PHRASE',
                'terminology_id',
                json_build_object(
                        '_type', 'TERMINOLOGY_ID',
                        'value', codephrase.terminology_id_value
                    ),
                'code_string', codephrase.code_string
            );
END
$$;
ALTER FUNCTION ehr.js_code_phrase(codephrase ehr.code_phrase) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_code_phrase(text, text) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    code_string ALIAS FOR $1;
    terminology ALIAS FOR $2;
BEGIN
    RETURN
        json_build_object(
                '_type', 'CODE_PHRASE',
                'terminology_id',
                json_build_object(
                        '_type', 'TERMINOLOGY_ID',
                        'value', terminology
                    ),
                'code_string', code_string
            );
END
$_$;
ALTER FUNCTION ehr.js_code_phrase(text, text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_composition(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    composition_uuid ALIAS FOR $1;
BEGIN
    RETURN (
        WITH composition_data AS (
            SELECT composition.language  as language,
                   composition.territory as territory,
                   composition.composer  as composer,
                   event_context.id      as context_id,
                   territory.twoletter   as territory_code,
                   entry.template_id     as template_id,
                   entry.archetype_id    as archetype_id,
                   concept.conceptid     as category_defining_code,
                   concept.description   as category_description,
                   entry.entry           as content
            FROM ehr.composition
                     INNER JOIN ehr.entry ON entry.composition_id = composition.id
                     LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
                     LEFT JOIN ehr.territory ON territory.code = composition.territory
                     LEFT JOIN ehr.concept ON concept.id = entry.category
            WHERE composition.id = composition_uuid
            LIMIT 1
        )
        SELECT jsonb_strip_nulls(
                       jsonb_build_object(
                               '_type', 'COMPOSITION',
                               'language', ehr.js_code_phrase(language, 'ISO_639-1'),
                               'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
                               'composer', ehr.js_party(composer),
                               'category',
                               ehr.js_dv_coded_text(category_description,
                                                    ehr.js_code_phrase(category_defining_code :: TEXT, 'openehr')),
                               'context', ehr.js_context(context_id),
                               'content', content
                           )
                   )
        FROM composition_data
    );
END
$_$;
ALTER FUNCTION ehr.js_composition(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_composition(uuid, server_node_id text) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    composition_uuid ALIAS FOR $1;
BEGIN
    RETURN (
        WITH entry_content AS (
            SELECT composition.id           as composition_id,
                   composition.language     as language,
                   composition.territory    as territory,
                   composition.composer     as composer,
                   composition.feeder_audit as feeder_audit,
                   composition.links        as links,
                   event_context.id         as context_id,
                   territory.twoletter      as territory_code,
                   entry.template_id        as template_id,
                   entry.archetype_id       as archetype_id,
                   entry.rm_version         as rm_version,
                   entry.entry              as content,
                   entry.category           as category,
                   entry.name               as name,
                   (SELECT jsonb_content
                    FROM (SELECT to_jsonb(jsonb_each(to_jsonb(jsonb_each((entry.entry)::jsonb)))) #>>
                                 '{value}' as jsonb_content) selcontent
                    WHERE jsonb_content::text like '{"%/content%'
                    LIMIT 1)                as json_content
            FROM ehr.composition
                     INNER JOIN ehr.entry ON entry.composition_id = composition.id
                     LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
                     LEFT JOIN ehr.territory ON territory.code = composition.territory
            WHERE composition.id = composition_uuid
        )
        SELECT jsonb_strip_nulls(
                       jsonb_build_object(
                               '_type', 'COMPOSITION',
                               'name', ehr.js_dv_text((entry_content.name).value),
                               'archetype_details',
                               ehr.js_archetype_details(entry_content.archetype_id, entry_content.template_id,
                                                        entry_content.rm_version),
                               'archetype_node_id', entry_content.archetype_id,
                               'feeder_audit', entry_content.feeder_audit,
                               'links', entry_content.links,
                               'uid',
                               ehr.js_object_version_id(ehr.composition_uid(entry_content.composition_id, server_node_id)),
                               'language', ehr.js_code_phrase(language, 'ISO_639-1'),
                               'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
                               'composer', ehr.js_canonical_party_identified(composer),
                               'category', ehr.js_dv_coded_text(category),
                               'context', ehr.js_context(context_id),
                               'content', entry_content.json_content::jsonb
                           )
                   )
        FROM entry_content
    );
END
$_$;
ALTER FUNCTION ehr.js_composition(uuid, server_node_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_concept(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    concept_id ALIAS FOR $1;
BEGIN

    IF (concept_id IS NULL) THEN
        RETURN NULL;
    END IF;

    RETURN (
        SELECT ehr.js_dv_coded_text(description, ehr.js_code_phrase(conceptid :: TEXT, 'openehr'))
        FROM ehr.concept
        WHERE id = concept_id
          AND language = 'en'
    );
END
$_$;
ALTER FUNCTION ehr.js_concept(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_context(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    context_id ALIAS FOR $1;
BEGIN

    IF (context_id IS NULL)
    THEN
        RETURN NULL;
    ELSE
        RETURN (
            WITH context_attributes AS (
                SELECT start_time,
                       start_time_tzid,
                       end_time,
                       end_time_tzid,
                       facility,
                       location,
                       other_context,
                       setting
                FROM ehr.event_context
                WHERE id = context_id
            )
            SELECT jsonb_strip_nulls(
                           jsonb_build_object(
                                   '_type', 'EVENT_CONTEXT',
                                   'start_time', ehr.js_dv_date_time(start_time, start_time_tzid),
                                   'end_time', ehr.js_dv_date_time(end_time, end_time_tzid),
                                   'location', location,
                                   'health_care_facility', ehr.js_canonical_party_identified(facility),
                                   'setting', ehr.js_dv_coded_text(setting),
                                   'other_context', other_context,
                                   'participations', ehr.js_participations(context_id)
                               )
                       )
            FROM context_attributes
        );
    END IF;
END
$_$;
ALTER FUNCTION ehr.js_context(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_context_setting(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    concept_id ALIAS FOR $1;
BEGIN

    IF (concept_id IS NULL) THEN
        RETURN NULL;
    END IF;

    RETURN (
        SELECT ehr.js_dv_coded_text(description, ehr.js_code_phrase(conceptid :: TEXT, 'openehr'))
        FROM ehr.concept
        WHERE id = concept_id
          AND language = 'en'
    );
END
$_$;
ALTER FUNCTION ehr.js_context_setting(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_contribution(uuid, text) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    contribution_uuid ALIAS FOR $1;
    server_id ALIAS FOR $2;
BEGIN
    RETURN (
        SELECT jsonb_strip_nulls(
                       jsonb_build_object(
                               '_type', 'CONTRIBUTION',
                               'uid', ehr.js_canonical_hier_object_id(contribution.id),
                               'audit', ehr.js_audit_details(contribution.has_audit)
                           )
                   )
        FROM ehr.contribution
        WHERE contribution.id = contribution_uuid
    );
END
$_$;
ALTER FUNCTION ehr.js_contribution(uuid, text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_dv_coded_text(dvcodedtext ehr.dv_coded_text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', (SELECT (
                                             CASE
                                                 WHEN ((dvcodedtext).defining_code IS NOT NULL)
                                                     THEN
                                                     'DV_CODED_TEXT'
                                                 ELSE
                                                     'DV_TEXT'
                                                 END
                                             )
                ),
                        'value', dvcodedtext.value,
                        'defining_code', ehr.js_code_phrase(dvcodedtext.defining_code),
                        'formatting', dvcodedtext.formatting,
                        'language', dvcodedtext.language,
                        'encoding', dvcodedtext.encoding,
                        'mappings', ehr.js_term_mappings(dvcodedtext.term_mapping)
                    )
            );
END
$$;
ALTER FUNCTION ehr.js_dv_coded_text(dvcodedtext ehr.dv_coded_text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_dv_coded_text(text, json) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    value_string ALIAS FOR $1;
    code_phrase ALIAS FOR $2;
BEGIN
    RETURN
        json_build_object(
                '_type', 'DV_CODED_TEXT',
                'value', value_string,
                'defining_code', code_phrase
            );
END
$_$;
ALTER FUNCTION ehr.js_dv_coded_text(text, json) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_dv_coded_text_inner(dvcodedtext ehr.dv_coded_text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        json_build_object(
                '_type', 'DV_CODED_TEXT',
                'value', dvcodedtext.value,
                'defining_code', ehr.js_code_phrase(dvcodedtext.defining_code)
            );
END
$$;
ALTER FUNCTION ehr.js_dv_coded_text_inner(dvcodedtext ehr.dv_coded_text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_dv_coded_text_inner(value text, terminology_id text, code_string text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        json_build_object(
                '_type', 'DV_CODED_TEXT',
                'value', value,
                'defining_code', ehr.js_code_phrase(code_string, terminology_id)
            );
END
$$;
ALTER FUNCTION ehr.js_dv_coded_text_inner(value text, terminology_id text, code_string text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_dv_date_time(timestamp without time zone, text) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    date_time ALIAS FOR $1;
    time_zone ALIAS FOR $2;
BEGIN

    IF (date_time IS NULL)
    THEN
        RETURN NULL;
    END IF;

    IF (time_zone IS NULL)
    THEN
        time_zone := '';
    END IF;

    RETURN
        json_build_object(
                '_type', 'DV_DATE_TIME',
                'value', to_char(date_time, 'YYYY-MM-DD"T"HH24:MI:SS.MS"' || time_zone || '"')
            );
END
$_$;
ALTER FUNCTION ehr.js_dv_date_time(timestamp without time zone, text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_dv_text(text) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    value_string ALIAS FOR $1;
BEGIN
    RETURN
        json_build_object(
                '_type', 'DV_TEXT',
                'value', value_string
            );
END
$_$;
ALTER FUNCTION ehr.js_dv_text(text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_ehr(uuid, text) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    ehr_uuid ALIAS FOR $1;
    server_id ALIAS FOR $2;
    contribution_json_array        JSONB[];
    contribution_details           JSONB;
    composition_version_json_array JSONB[];
    composition_in_ehr_id          RECORD;
    folder_version_json_array      JSONB[];
    folder_in_ehr_id               RECORD;
BEGIN

    FOR contribution_details IN (SELECT ehr.js_contribution(contribution.id, server_id)
                                 FROM ehr.contribution
                                 WHERE contribution.ehr_id = ehr_uuid
                                   AND contribution.contribution_type != 'ehr')
        LOOP
            contribution_json_array := array_append(contribution_json_array, contribution_details);
        END LOOP;

    FOR composition_in_ehr_id IN (SELECT composition.id, composition.sys_transaction
                                  FROM ehr.composition
                                  WHERE composition.ehr_id = ehr_uuid)
        LOOP
            composition_version_json_array := array_append(
                    composition_version_json_array,
                    jsonb_build_object(
                            '_type', 'VERSIONED_COMPOSITION',
                            'id', ehr.js_object_version_id(ehr.composition_uid(composition_in_ehr_id.id, server_id)),
                            'time_created', ehr.js_dv_date_time(composition_in_ehr_id.sys_transaction, 'Z')
                        )
                );
        END LOOP;

    FOR folder_in_ehr_id IN (SELECT folder.id, folder.sys_transaction
                             FROM ehr.folder
                                      JOIN ehr.contribution ON folder.in_contribution = contribution.id
                             WHERE contribution.ehr_id = ehr_uuid)
        LOOP
            folder_version_json_array := array_append(
                    folder_version_json_array,
                    ehr.js_folder(folder_in_ehr_id.id, server_id)
                );
        END LOOP;

    RETURN (
        WITH ehr_data AS (
            SELECT ehr.id                as ehr_id,
                   ehr.date_created      as date_created,
                   ehr.date_created_tzid as date_created_tz,
                   ehr.access            as access,
                   system.settings       as system_value,
                   ehr.directory         as directory
            FROM ehr.ehr
                     JOIN ehr.system ON system.id = ehr.system_id
            WHERE ehr.id = ehr_uuid
        )
        SELECT jsonb_strip_nulls(
                       jsonb_build_object(
                               '_type', 'EHR',
                               'ehr_id', ehr.js_canonical_hier_object_id(ehr_data.ehr_id),
                               'system_id', ehr.js_canonical_hier_object_id(ehr_data.system_value),
                               'ehr_status', ehr.js_ehr_status(ehr_data.ehr_id, server_id),
                               'time_created', ehr.js_dv_date_time(ehr_data.date_created, ehr_data.date_created_tz),
                               'contributions', contribution_json_array,
                               'compositions', composition_version_json_array,
                               'folders', folder_version_json_array,
                               'directory', ehr.js_folder(directory, server_id)
                           )
                   -- 'ehr_access'
                   -- 'tags'
                   )

        FROM ehr_data
    );
END
$_$;
ALTER FUNCTION ehr.js_ehr(uuid, text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_ehr_status(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    ehr_uuid ALIAS FOR $1;
BEGIN
    RETURN (
        WITH ehr_status_data AS (
            SELECT status.other_details     as other_details,
                   status.party             as subject,
                   status.is_queryable      as is_queryable,
                   status.is_modifiable     as is_modifiable,
                   status.sys_transaction   as time_created,
                   status.name              as status_name,
                   status.archetype_node_id as archetype_node_id
            FROM ehr.status
            WHERE status.ehr_id = ehr_uuid
            LIMIT 1
        )
        SELECT jsonb_strip_nulls(
                       jsonb_build_object(
                               '_type', 'EHR_STATUS',
                               'archetype_node_id', archetype_node_id,
                               'name', status_name,
                               'subject', ehr.js_party_self(subject),
                               'is_queryable', is_queryable,
                               'is_modifiable', is_modifiable,
                               'other_details', other_details
                           )
                   )
        FROM ehr_status_data
    );
END
$_$;
ALTER FUNCTION ehr.js_ehr_status(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_ehr_status(ehr_uuid uuid, server_id text) RETURNS json
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        WITH ehr_status_data AS (
            SELECT status.other_details     as other_details,
                   status.party             as subject,
                   status.is_queryable      as is_queryable,
                   status.is_modifiable     as is_modifiable,
                   status.sys_transaction   as time_created,
                   status.name              as status_name,
                   status.archetype_node_id as archetype_node_id
            FROM ehr.status
            WHERE status.ehr_id = ehr_uuid
            LIMIT 1
        )
        SELECT jsonb_strip_nulls(
                       jsonb_build_object(
                               '_type', 'EHR_STATUS',
                               'archetype_node_id', archetype_node_id,
                               'name', status_name,
                               'subject', ehr.js_canonical_party_identified(subject),
                               'uid', ehr.js_ehr_status_uid(ehr_uuid, server_id),
                               'is_queryable', is_queryable,
                               'is_modifiable', is_modifiable,
                               'other_details', other_details
                           )
                   )
        FROM ehr_status_data
    );
END
$$;
ALTER FUNCTION ehr.js_ehr_status(ehr_uuid uuid, server_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_ehr_status_uid(ehr_uuid uuid, server_id text) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN jsonb_strip_nulls(
            jsonb_build_object(
                    '_type', 'HIER_OBJECT_ID',
                    'value', ehr.ehr_status_uid(ehr_uuid, server_id)
                )
        );
END
$$;
ALTER FUNCTION ehr.js_ehr_status_uid(ehr_uuid uuid, server_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_folder(folder_uid uuid, server_id text) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN

    IF (NOT EXISTS(SELECT * FROM ehr.folder WHERE id = folder_uid)) THEN
        RETURN NULL;
    end if;

    RETURN (
        WITH folder_data AS (
            SELECT name, sys_transaction
            FROM ehr.folder
            WHERE id = folder_uid
        )
        SELECT jsonb_build_object(
                       '_type', 'VERSIONED_FOLDER',
                       'id', ehr.js_object_version_id(ehr.folder_uid(folder_uid, server_id)),
                       'name', ehr.js_dv_text(folder_data.name),
                       'time_created', ehr.js_dv_date_time(folder_data.sys_transaction, 'Z')
                   )
        FROM folder_data
    );
END
$$;
ALTER FUNCTION ehr.js_folder(folder_uid uuid, server_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_object_version_id(version_id text) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', 'OBJECT_VERSION_ID',
                        'value', version_id
                    )
            );
END
$$;
ALTER FUNCTION ehr.js_object_version_id(version_id text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_participations(event_context_id uuid) RETURNS jsonb[]
    LANGUAGE plpgsql
AS
$$
DECLARE
    item               JSONB;
    arr                JSONB[];
    participation_data RECORD;
BEGIN

    FOR participation_data IN
        SELECT participation.performer as performer,
               participation.function  as function,
               participation.mode      as mode,
               participation.time_lower,
               participation.time_lower_tz,
               participation.time_upper,
               participation.time_upper_tz
        FROM ehr.participation
        WHERE event_context = event_context_id
        LOOP
            item :=
                    jsonb_strip_nulls(
                            jsonb_build_object(
                                    '_type', 'PARTICIPATION',
                                    'function', (SELECT (
                                                            CASE
                                                                WHEN ((participation_data.function).defining_code IS NOT NULL)
                                                                    THEN
                                                                    ehr.js_dv_coded_text_inner(participation_data.function)
                                                                ELSE
                                                                    ehr.js_dv_text((participation_data.function).value)
                                                                END
                                                            )
                                    ),
                                    'performer', ehr.js_canonical_party_identified(participation_data.performer),
                                    'mode', ehr.js_dv_coded_text_inner(participation_data.mode),
                                    'time', (SELECT (
                                                        CASE
                                                            WHEN (participation_data.time_lower IS NOT NULL OR
                                                                  participation_data.time_upper IS NOT NULL) THEN
                                                                jsonb_build_object(
                                                                        '_type', 'DV_INTERVAL',
                                                                        'lower', ehr.js_dv_date_time(
                                                                                participation_data.time_lower,
                                                                                participation_data.time_lower_tz),
                                                                        'upper', ehr.js_dv_date_time(
                                                                                participation_data.time_upper,
                                                                                participation_data.time_upper_tz)
                                                                    )
                                                            ELSE
                                                                NULL
                                                            END
                                                        )
                                    )
                                )
                        );
            arr := array_append(arr, item);
        END LOOP;
    RETURN arr;
END
$$;
ALTER FUNCTION ehr.js_participations(event_context_id uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_party(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    party_id ALIAS FOR $1;
BEGIN
    RETURN (
        SELECT ehr.js_party_identified(name,
                                       ehr.js_party_ref(party_ref_value, party_ref_scheme, party_ref_namespace,
                                                        party_ref_type))
        FROM ehr.party_identified
        WHERE id = party_id
    );
END
$_$;
ALTER FUNCTION ehr.js_party(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_party_identified(text, json) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    name_value ALIAS FOR $1;
    external_ref ALIAS FOR $2;
BEGIN
    IF (external_ref IS NOT NULL) THEN
        RETURN
            json_build_object(
                    '_type', 'PARTY_IDENTIFIED',
                    'name', name_value,
                    'external_ref', external_ref
                );
    ELSE
        RETURN
            json_build_object(
                    '_type', 'PARTY_IDENTIFIED',
                    'name', name_value
                );
    END IF;
END
$_$;
ALTER FUNCTION ehr.js_party_identified(text, json) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_party_ref(text, text, text, text) RETURNS json
    LANGUAGE plpgsql
    IMMUTABLE
AS
$_$
DECLARE
    id_value ALIAS FOR $1;
    id_scheme ALIAS FOR $2;
    namespace ALIAS FOR $3;
    party_type ALIAS FOR $4;
BEGIN

    IF (id_value IS NULL AND id_scheme IS NULL AND namespace IS NULL AND party_type IS NULL) THEN
        RETURN NULL;
    ELSE
        RETURN
            json_build_object(
                    '_type', 'PARTY_REF',
                    'id',
                    json_build_object(
                            '_type', 'GENERIC_ID',
                            'value', id_value,
                            'scheme', id_scheme
                        ),
                    'namespace', namespace,
                    'type', party_type
                );
    END IF;
END
$_$;
ALTER FUNCTION ehr.js_party_ref(text, text, text, text) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_party_self(uuid) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    party_id ALIAS FOR $1;
BEGIN
    RETURN (
        SELECT ehr.js_party_self_identified(name,
                                            ehr.js_party_ref(party_ref_value, party_ref_scheme, party_ref_namespace,
                                                             party_ref_type))
        FROM ehr.party_identified
        WHERE id = party_id
    );
END
$_$;
ALTER FUNCTION ehr.js_party_self(uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_party_self_identified(text, json) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    name_value ALIAS FOR $1;
    external_ref ALIAS FOR $2;
BEGIN
    IF (external_ref IS NOT NULL) THEN
        RETURN
            json_build_object(
                    '_type', 'PARTY_SELF',
                    'external_ref', external_ref
                );
    ELSE
        RETURN
            json_build_object(
                    '_type', 'PARTY_SELF'
                );
    END IF;
END
$_$;
ALTER FUNCTION ehr.js_party_self_identified(text, json) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_term_mappings(mappings text[]) RETURNS jsonb[]
    LANGUAGE plpgsql
AS
$$
DECLARE
    encoded    TEXT;
    attributes TEXT[];
    item       JSONB;
    arr        JSONB[];
BEGIN

    IF (mappings IS NULL) THEN
        RETURN NULL;
    end if;

    FOREACH encoded IN ARRAY mappings
        LOOP
        -- 	  RAISE NOTICE 'encoded %',encoded;
        -- the encoding is required since ARRAY in PG only support base types (e.g. no UDTs)
            attributes := regexp_split_to_array(encoded, '\|');
            item := jsonb_build_object(
                    '_type', 'TERM_MAPPING',
                    'match', attributes[1],
                    'purpose', ehr.js_dv_coded_text_inner(attributes[2], attributes[3], attributes[4]),
                    'target', ehr.js_code_phrase(attributes[6], attributes[5])
                );
            arr := array_append(arr, item);
        END LOOP;
    RETURN arr;
END
$$;
ALTER FUNCTION ehr.js_term_mappings(mappings text[]) OWNER TO ehrbase;

CREATE FUNCTION ehr.js_typed_element_value(jsonb) RETURNS jsonb
    LANGUAGE plpgsql
AS
$_$
DECLARE
    element_value ALIAS FOR $1;
BEGIN
    RETURN (
        SELECT jsonb_strip_nulls(
                           (element_value #>> '{/value}')::jsonb ||
                           jsonb_build_object(
                                   '_type',
                                   upper(ehr.camel_to_snake(element_value #>> '{/$CLASS$}'))
                               )
                   )
    );
END
$_$;
ALTER FUNCTION ehr.js_typed_element_value(jsonb) OWNER TO ehrbase;

CREATE FUNCTION ehr.json_entry_migrate(jsonb_entry jsonb, OUT out_composition_name text,
                                       OUT out_new_entry jsonb) RETURNS record
    LANGUAGE plpgsql
AS
$$
DECLARE
    composition_name TEXT;
    composition_idx  int;
    str_left         text;
    str_right        text;
    new_entry        jsonb;
BEGIN

    composition_idx := strpos(jsonb_entry::text, 'and name/value=');
    str_left := left(jsonb_entry::text, composition_idx - 2);
    -- get the right part from 'and name/value'
    str_right := substr(jsonb_entry::text, composition_idx + 16);
    composition_idx := strpos(str_right, ']'); -- skip the name
    composition_name := left(str_right, composition_idx - 2); -- remove trailing single-quote, closing bracket
    str_right := substr(str_right::text, composition_idx);

    new_entry := (str_left || str_right)::jsonb;

    SELECT composition_name, new_entry INTO out_composition_name, out_new_entry;

    -- 	RAISE NOTICE 'left : %, right: %', str_left, str_right;

END
$$;
ALTER FUNCTION ehr.json_entry_migrate(jsonb_entry jsonb, OUT out_composition_name text, OUT out_new_entry jsonb) OWNER TO ehrbase;

CREATE FUNCTION ehr.json_party_identified(name text, refid uuid, namespace text, ref_type text, scheme text,
                                          id_value text) RETURNS json
    LANGUAGE plpgsql
AS
$$
DECLARE
    json_party_struct JSON;
BEGIN
    SELECT jsonb_strip_nulls(
                   jsonb_build_object(
                           '_type', 'PARTY_IDENTIFIED',
                           'name', name,
                           'identifiers',
                           jsonb_build_array(
                                   jsonb_build_object(
                                           '_type', 'DV_IDENTIFIER',
                                           'id', refid
                                       )
                               ),
                           'external_ref', jsonb_build_object(
                                   '_type', 'PARTY_REF',
                                   'namespace', namespace,
                                   'type', ref_type,
                                   'id', ehr.js_canonical_generic_id(scheme, id_value)
                               )
                       )
               )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$;
ALTER FUNCTION ehr.json_party_identified(name text, refid uuid, namespace text, ref_type text, scheme text, id_value text) OWNER TO ehrbase;

CREATE FUNCTION ehr.json_party_identified(name text, refid uuid, namespace text, ref_type text, scheme text,
                                          id_value text, objectid_type ehr.party_ref_id_type) RETURNS json
    LANGUAGE plpgsql
AS
$$
DECLARE
    json_party_struct    JSON;
    item                 JSONB;
    arr                  JSONB[];
    identifier_attribute record;
BEGIN
    -- build an array of json object from identifiers if any
    FOR identifier_attribute IN SELECT * FROM ehr.identifier WHERE identifier.party = refid
        LOOP
            item := jsonb_build_object(
                    '_type', 'DV_IDENTIFIER',
                    'id', identifier_attribute.id_value,
                    'assigner', identifier_attribute.assigner,
                    'issuer', identifier_attribute.issuer,
                    'type', identifier_attribute.type_name
                );
            arr := array_append(arr, item);
        END LOOP;

    SELECT jsonb_strip_nulls(
                   jsonb_build_object(
                           '_type', 'PARTY_IDENTIFIED',
                           'name', name,
                           'identifiers', arr,
                           'external_ref', ehr.party_ref(namespace, ref_type, scheme, id_value, objectid_type)
                       )
               )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$;
ALTER FUNCTION ehr.json_party_identified(name text, refid uuid, namespace text, ref_type text, scheme text, id_value text, objectid_type ehr.party_ref_id_type) OWNER TO ehrbase;

CREATE FUNCTION ehr.json_party_related(name text, refid uuid, namespace text, ref_type text, scheme text, id_value text,
                                       objectid_type ehr.party_ref_id_type, relationship ehr.dv_coded_text) RETURNS json
    LANGUAGE plpgsql
AS
$$
DECLARE
    json_party_struct    JSON;
    item                 JSONB;
    arr                  JSONB[];
    identifier_attribute record;
BEGIN
    -- build an array of json object from identifiers if any
    FOR identifier_attribute IN SELECT * FROM ehr.identifier WHERE identifier.party = refid
        LOOP
            item := jsonb_build_object(
                    '_type', 'DV_IDENTIFIER',
                    'id', identifier_attribute.id_value
                        'assigner', identifier_attribute.assigner,
                    'issuer', identifier_attribute.issuer,
                    'type', identifier_attribute.type_name
                );
            arr := array_append(arr, item);
        END LOOP;

    SELECT jsonb_strip_nulls(
                   jsonb_build_object(
                           '_type', 'PARTY_RELATED',
                           'name', name,
                           'identifiers', arr,
                           'external_ref', ehr.party_ref(namespace, ref_type, scheme, id_value, objectid_type),
                           'relationship', ehr.js_dv_coded_text(relationship)
                       )
               )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$;
ALTER FUNCTION ehr.json_party_related(name text, refid uuid, namespace text, ref_type text, scheme text, id_value text, objectid_type ehr.party_ref_id_type, relationship ehr.dv_coded_text) OWNER TO ehrbase;

CREATE FUNCTION ehr.json_party_self(refid uuid, namespace text, ref_type text, scheme text, id_value text,
                                    objectid_type ehr.party_ref_id_type) RETURNS json
    LANGUAGE plpgsql
AS
$$
DECLARE
    json_party_struct JSON;
BEGIN
    SELECT jsonb_strip_nulls(
                   jsonb_build_object(
                           '_type', 'PARTY_SELF',
                           'external_ref', ehr.party_ref(namespace, ref_type, scheme, id_value, objectid_type)
                       )
               )
    INTO json_party_struct;
    RETURN json_party_struct;
end;
$$;
ALTER FUNCTION ehr.json_party_self(refid uuid, namespace text, ref_type text, scheme text, id_value text, objectid_type ehr.party_ref_id_type) OWNER TO ehrbase;

CREATE FUNCTION ehr.jsonb_array_elements(jsonb_val jsonb) RETURNS SETOF jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY SELECT jsonb_array_elements(jsonb_val);
END
$$;
ALTER FUNCTION ehr.jsonb_array_elements(jsonb_val jsonb) OWNER TO ehrbase;

CREATE FUNCTION ehr.jsonb_extract_path(from_json jsonb, VARIADIC path_elems text[]) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN jsonb_extract_path(from_json, path_elems);
END
$$;
ALTER FUNCTION ehr.jsonb_extract_path(from_json jsonb, VARIADIC path_elems text[]) OWNER TO ehrbase;

CREATE FUNCTION ehr.jsonb_extract_path_text(from_json jsonb, VARIADIC path_elems text[]) RETURNS text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN jsonb_extract_path_text(from_json, path_elems);
END
$$;
ALTER FUNCTION ehr.jsonb_extract_path_text(from_json jsonb, VARIADIC path_elems text[]) OWNER TO ehrbase;

CREATE FUNCTION ehr.map_change_type_to_codestring(literal text) RETURNS text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        CASE
            WHEN literal = 'creation' THEN '249'
            WHEN literal = 'amendment' THEN '250'
            WHEN literal = 'modification' THEN '251'
            WHEN literal = 'synthesis' THEN '252'
            WHEN literal = 'deleted' THEN '523'
            WHEN literal = 'attestation' THEN '666'
            WHEN literal = 'unknown' THEN '253'
            ELSE
                '253'
            END
        );
END
$$;
ALTER FUNCTION ehr.map_change_type_to_codestring(literal text) OWNER TO ehrbase;

CREATE FUNCTION ehr.migrate_concept_to_dv_coded_text(concept_id uuid) RETURNS ehr.dv_coded_text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        WITH concept_val AS (
            SELECT conceptid as code,
                   description
            FROM ehr.concept
            WHERE concept.id = concept_id
            LIMIT 1
        )
        select (concept_val.code, ('openehr', concept_val.description)::ehr.code_phrase, null, null, null,
                null)::ehr.dv_coded_text
        from concept_val
    );
END
$$;
ALTER FUNCTION ehr.migrate_concept_to_dv_coded_text(concept_id uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.migrate_folder_audit(OUT ret_id uuid) RETURNS uuid
    LANGUAGE plpgsql
AS
$$
BEGIN
    -- Add migration dummy party entry, only if not existing already
    INSERT INTO ehr.party_identified (
        -- id will get generated
        name,
        party_type,
        object_id_type)
    SELECT 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0',
           'party_self',
           'undefined'
    WHERE NOT EXISTS(
            SELECT 1 FROM ehr.party_identified WHERE name = 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0'
        );

    -- Helper queries to:
    -- 1) Find the oldest audit to copy two attributes from
    -- (Note: There will always be an audit, because this migration function is only run for existing folder, which require and EHR, which will have a Status, which will have an Audit.
    WITH audits AS (
        SELECT ad.system_id,
               ad.time_committed_tzid
        FROM ehr.audit_details AS ad
        WHERE ad.id IN (
            SELECT id
            FROM ehr.audit_details
            ORDER BY time_committed asc
            LIMIT 1
        )
    ),
         -- 2) Find the dummy party
         party AS (
             SELECT id
             FROM ehr.party_identified
             WHERE name = 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0'
             LIMIT 1
         )

         -- Copy the values of the oldest/initial audit
         -- and change committer to the dummy party and the description to "migration_dummy"
    INSERT
    INTO ehr.audit_details (
        -- id will get generated
        system_id,
        committer,
        -- time_committed will get default value
        time_committed_tzid,
        change_type,
        description)
    SELECT a.system_id,
           p.id,             -- set dummy committer
           a.time_committed_tzid,
           'Unknown',        -- change type set to unknown
           'migration_dummy' -- description to mark entry as dummy
    FROM audits AS a,
         party AS p

         -- Finally take and return the ID of the inserted row
    RETURNING id INTO ret_id; -- returned at the end automatically
END
$$;
ALTER FUNCTION ehr.migrate_folder_audit(OUT ret_id uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.migrate_participation_function(mode text) RETURNS ehr.dv_coded_text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (mode, NULL, NULL, NULL, NULL)::ehr.dv_coded_text;
END
$$;
ALTER FUNCTION ehr.migrate_participation_function(mode text) OWNER TO ehrbase;

CREATE FUNCTION ehr.migrate_participation_mode(mode text) RETURNS ehr.dv_coded_text
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        WITH dv_coded_text_attributes AS (
            WITH mode_split AS (
                select regexp_split_to_array((
                                                 (regexp_split_to_array(mode, '{|}'))[2]), ',')
                           as arr
            )
            select (regexp_split_to_array(arr[1], '='))[2] as code_string,
                   (regexp_split_to_array(arr[2], '='))[2] as terminology_id,
                   (regexp_split_to_array(arr[3], '='))[2] as value
            from mode_split
        )
        select (value, (terminology_id, code_string)::ehr.code_phrase, null, null, null)::ehr.dv_coded_text
        from dv_coded_text_attributes
    );
END
$$;
ALTER FUNCTION ehr.migrate_participation_mode(mode text) OWNER TO ehrbase;

CREATE FUNCTION ehr.migration_audit_committer(committer uuid) RETURNS uuid
    LANGUAGE plpgsql
AS
$$
BEGIN

    -- Add migration dummy party entry, only if not existing already
    INSERT INTO ehr.party_identified (
        -- id will get generated
        name,
        party_type,
        object_id_type)
    SELECT 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0',
           'party_self',
           'undefined'
    WHERE NOT EXISTS(
            SELECT 1
            FROM ehr.party_identified
            WHERE name = 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0'
        );

    IF
        committer IS NULL THEN
        RETURN (
            SELECT id
            FROM ehr.party_identified
            WHERE name = 'migration_dummy_65a3da3a-476f-4b1e-ab04-fa1c42edeac0'
            LIMIT 1
        );
    ELSE
        RETURN committer;
    END IF;

END
$$;
ALTER FUNCTION ehr.migration_audit_committer(committer uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.migration_audit_system_id(system_id uuid) RETURNS uuid
    LANGUAGE plpgsql
AS
$$
BEGIN

    -- Add migration dummy system entry, only if not existing already
    INSERT INTO ehr.system (
        -- id will get generated
        description,
        settings)
    SELECT 'migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e',
           'internal.migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e.org'
    WHERE NOT EXISTS(
            SELECT 1
            FROM ehr.system
            WHERE description = 'migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e'
        );

    IF
        system_id IS NULL THEN
        RETURN (
            SELECT id
            FROM ehr.system
            WHERE description = 'migration_dummy_96715295-b63a-4d5e-b3d1-7da8bb6edb2e'
            LIMIT 1
        );
    ELSE
        RETURN system_id;
    END IF;

END
$$;
ALTER FUNCTION ehr.migration_audit_system_id(system_id uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.migration_audit_tzid(time_committed_tzid text) RETURNS text
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF
        time_committed_tzid IS NULL THEN
        RETURN (
            'Etc/UTC'
            );
    ELSE
        RETURN time_committed_tzid;
    END IF;
END
$$;
ALTER FUNCTION ehr.migration_audit_tzid(time_committed_tzid text) OWNER TO ehrbase;

CREATE FUNCTION ehr.object_version_id(uuid, text, integer) RETURNS json
    LANGUAGE plpgsql
AS
$_$
DECLARE
    object_uuid ALIAS FOR $1;
    object_host ALIAS FOR $2;
    object_version ALIAS FOR $3;
BEGIN
    RETURN
        json_build_object(
                '_type', 'OBJECT_VERSION_ID',
                'value', object_uuid::TEXT || '::' || object_host || '::' || object_version::TEXT
            );
END
$_$;
ALTER FUNCTION ehr.object_version_id(uuid, text, integer) OWNER TO ehrbase;

CREATE FUNCTION ehr.party_ref(namespace text, ref_type text, scheme text, id_value text,
                              objectid_type ehr.party_ref_id_type) RETURNS jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN
        (SELECT (
                    CASE
                        WHEN (namespace IS NOT NULL AND ref_type IS NOT NULL) THEN
                            jsonb_build_object(
                                    '_type', 'PARTY_REF',
                                    'namespace', namespace,
                                    'type', ref_type,
                                    'id',
                                    ehr.js_canonical_object_id(objectid_type, scheme, id_value)
                                )
                        ELSE NULL
                        END
                    )
        );
END;
$$;
ALTER FUNCTION ehr.party_ref(namespace text, ref_type text, scheme text, id_value text, objectid_type ehr.party_ref_id_type) OWNER TO ehrbase;

CREATE FUNCTION ehr.party_usage(party_uuid uuid) RETURNS bigint
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN (
        with usage_uuid as (
            SELECT facility as uuid
            from ehr.event_context
            where facility = party_uuid
            UNION
            SELECT facility as uuid
            from ehr.event_context_history
            where facility = party_uuid
            UNION
            SELECT composer as uuid
            from ehr.composition
            where composer = party_uuid
            UNION
            SELECT composer as uuid
            from ehr.composition_history
            where composer = party_uuid
            UNION
            SELECT performer as uuid
            from ehr.participation
            where performer = party_uuid
            UNION
            SELECT performer as uuid
            from ehr.participation_history
            where performer = party_uuid
            UNION
            SELECT party as uuid
            from ehr.status
            where party = party_uuid
            UNION
            SELECT party as uuid
            from ehr.status_history
            where party = party_uuid
            UNION
            SELECT committer as uuid
            from ehr.audit_details
            where committer = party_uuid
        )
        SELECT count(usage_uuid.uuid)
        FROM usage_uuid
    );
END
$$;
ALTER FUNCTION ehr.party_usage(party_uuid uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.party_usage_identification(party_uuid uuid)
    RETURNS TABLE
            (
                id     uuid,
                entity text
            )
    LANGUAGE sql
AS
$$
with usage_uuid as (
    SELECT facility as uuid, 'FACILITY' as entity
    from ehr.event_context
    where facility = party_uuid
    UNION
    SELECT facility as uuid, 'FACILITY_HISTORY' as entity
    from ehr.event_context_history
    where facility = party_uuid
    UNION
    SELECT composer as uuid, 'COMPOSER' as entity
    from ehr.composition
    where composer = party_uuid
    UNION
    SELECT composer as uuid, 'COMPOSER_HISTORY' as entity
    from ehr.composition_history
    where composer = party_uuid
    UNION
    SELECT performer as uuid, 'PERFORMER' as entity
    from ehr.participation
    where performer = party_uuid
    UNION
    SELECT performer as uuid, 'PERFORMER_HISTORY' as entity
    from ehr.participation_history
    where performer = party_uuid
    UNION
    SELECT party as uuid, 'SUBJECT' as entity
    from ehr.status
    where party = party_uuid
    UNION
    SELECT party as uuid, 'SUBJECT_HISTORY' as entity
    from ehr.status_history
    where party = party_uuid
    UNION
    SELECT committer as uuid, 'AUDIT_DETAILS' as entity
    from ehr.audit_details
    where committer = party_uuid
)
SELECT usage_uuid.uuid, usage_uuid.entity
FROM usage_uuid;
$$;
ALTER FUNCTION ehr.party_usage_identification(party_uuid uuid) OWNER TO ehrbase;

CREATE FUNCTION ehr.tr_function_delete_folder_item() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    DELETE
    FROM ehr.object_ref
    WHERE ehr.object_ref.id = OLD.object_ref_id
      AND ehr.object_ref.in_contribution = OLD.in_contribution;
    RETURN OLD;
END;
$$;
ALTER FUNCTION ehr.tr_function_delete_folder_item() OWNER TO ehrbase;
COMMENT ON FUNCTION ehr.tr_function_delete_folder_item() IS 'fires after deletion of folder_items when the corresponding Object_ref  needs to be deleted.';

CREATE FUNCTION ehr.xjsonb_array_elements(entry jsonb) RETURNS SETOF jsonb
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF (entry IS NULL) THEN
        RETURN QUERY SELECT NULL::jsonb ;
    ELSE
        RETURN QUERY SELECT jsonb_array_elements(entry);
    END IF;

END
$$;
ALTER FUNCTION ehr.xjsonb_array_elements(entry jsonb) OWNER TO ehrbase;

-- EHRbase TABLES ------------------------------------------------------------------------------------------------------



CREATE TABLE ehr.access
(
    id       uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    settings text,
    scheme   text
);
ALTER TABLE ehr.access
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.access
    ADD CONSTRAINT access_pkey PRIMARY KEY (id);

COMMENT ON TABLE ehr.access IS 'defines the modality for accessing an com.ethercis.ehr (security strategy implementation)';

CREATE TABLE ehr.attestation
(
    id         uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    proof      text,
    reason     text,
    is_pending boolean,
    has_audit  uuid                                NOT NULL,
    reference  uuid                                NOT NULL
);
ALTER TABLE ehr.attestation
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.attestation
    ADD CONSTRAINT attestation_pkey PRIMARY KEY (id);

CREATE TABLE ehr.attestation_ref
(
    ref uuid DEFAULT ext.uuid_generate_v4() NOT NULL
);
ALTER TABLE ehr.attestation_ref
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.attestation_ref
    ADD CONSTRAINT attestation_ref_pkey PRIMARY KEY (ref);

ALTER TABLE ONLY ehr.attestation
    ADD CONSTRAINT attestation_reference_fkey FOREIGN KEY (reference) REFERENCES ehr.attestation_ref (ref) ON DELETE CASCADE;

CREATE TABLE ehr.attested_view
(
    id                        uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    attestation_id            uuid,
    alternate_text            text,
    compression_algorithm     text,
    media_type                text,
    data                      bytea,
    integrity_check           bytea,
    integrity_check_algorithm text,
    thumbnail                 uuid,
    uri                       text
);
ALTER TABLE ehr.attested_view
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.attested_view
    ADD CONSTRAINT attested_view_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.attested_view
    ADD CONSTRAINT attested_view_attestation_id_fkey FOREIGN KEY (attestation_id) REFERENCES ehr.attestation (id) ON DELETE CASCADE;


CREATE TABLE ehr.audit_details
(
    id                  uuid                        DEFAULT ext.uuid_generate_v4() NOT NULL,
    system_id           uuid                                                       NOT NULL,
    committer           uuid                                                       NOT NULL,
    time_committed      timestamp without time zone DEFAULT now(),
    time_committed_tzid text                                                       NOT NULL,
    change_type         ehr.contribution_change_type                               NOT NULL,
    description         text
);
ALTER TABLE ehr.audit_details
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.audit_details
    ADD CONSTRAINT audit_details_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.attestation
    ADD CONSTRAINT attestation_has_audit_fkey FOREIGN KEY (has_audit) REFERENCES ehr.audit_details (id) ON DELETE CASCADE;

CREATE TABLE ehr.composition
(
    id              uuid    DEFAULT ext.uuid_generate_v4() NOT NULL,
    ehr_id          uuid,
    in_contribution uuid,
    active          boolean DEFAULT true,
    is_persistent   boolean DEFAULT true,
    language        character varying(5),
    territory       integer,
    composer        uuid                                   NOT NULL,
    sys_transaction timestamp without time zone            NOT NULL,
    sys_period      tstzrange                              NOT NULL,
    has_audit       uuid,
    attestation_ref uuid,
    feeder_audit    jsonb,
    links           jsonb
);
ALTER TABLE ehr.composition
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_pkey PRIMARY KEY (id);

COMMENT ON TABLE ehr.composition IS 'Composition table';

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_attestation_ref_fkey FOREIGN KEY (attestation_ref) REFERENCES ehr.attestation_ref (ref) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_has_audit_fkey FOREIGN KEY (has_audit) REFERENCES ehr.audit_details (id) ON DELETE CASCADE;

CREATE TABLE ehr.ehr
(
    id                uuid                        DEFAULT ext.uuid_generate_v4() NOT NULL,
    date_created      timestamp without time zone DEFAULT CURRENT_DATE,
    date_created_tzid text,
    access            uuid,
    system_id         uuid,
    directory         uuid
);
ALTER TABLE ehr.ehr
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.ehr
    ADD CONSTRAINT ehr_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.ehr
    ADD CONSTRAINT ehr_access_fkey FOREIGN KEY (access) REFERENCES ehr.access (id);

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_ehr_id_fkey FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id) ON DELETE CASCADE;

COMMENT ON TABLE ehr.ehr IS 'EHR itself';

CREATE TABLE ehr.entry
(
    id              uuid              DEFAULT ext.uuid_generate_v4()                                                                                                                NOT NULL,
    composition_id  uuid,
    sequence        integer,
    item_type       ehr.entry_type,
    template_id     text,
    template_uuid   uuid,
    archetype_id    text,
    category        ehr.dv_coded_text,
    entry           jsonb,
    sys_transaction timestamp without time zone                                                                                                                                     NOT NULL,
    sys_period      tstzrange                                                                                                                                                       NOT NULL,
    rm_version      text              DEFAULT '1.0.4'::text                                                                                                                         NOT NULL,
    name            ehr.dv_coded_text DEFAULT ROW ('_DEFAULT_NAME'::text, NULL::ehr.code_phrase, NULL::text, NULL::ehr.code_phrase, NULL::ehr.code_phrase, NULL)::ehr.dv_coded_text NOT NULL
);
ALTER TABLE ehr.entry
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.entry
    ADD CONSTRAINT entry_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.entry
    ADD CONSTRAINT entry_composition_id_fkey FOREIGN KEY (composition_id) REFERENCES ehr.composition (id) ON DELETE CASCADE;

COMMENT ON TABLE ehr.entry IS 'this table hold the actual archetyped data values (fromBinder a template)';

CREATE TABLE ehr.event_context
(
    id              uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    composition_id  uuid,
    start_time      timestamp without time zone         NOT NULL,
    start_time_tzid text,
    end_time        timestamp without time zone,
    end_time_tzid   text,
    facility        uuid,
    location        text,
    other_context   jsonb,
    setting         ehr.dv_coded_text,
    sys_transaction timestamp without time zone         NOT NULL,
    sys_period      tstzrange                           NOT NULL
);
ALTER TABLE ehr.event_context
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.event_context
    ADD CONSTRAINT event_context_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.event_context
    ADD CONSTRAINT event_context_composition_id_fkey FOREIGN KEY (composition_id) REFERENCES ehr.composition (id) ON DELETE CASCADE;

COMMENT ON TABLE ehr.event_context IS 'defines the context of an event (time, who, where... see openEHR IM 5.2';

CREATE TABLE ehr.party_identified
(
    id                  uuid                  DEFAULT ext.uuid_generate_v4() NOT NULL,
    name                text,
    party_ref_value     text,
    party_ref_scheme    text,
    party_ref_namespace text,
    party_ref_type      text,
    party_type          ehr.party_type        DEFAULT 'party_identified'::ehr.party_type,
    relationship        ehr.dv_coded_text,
    object_id_type      ehr.party_ref_id_type DEFAULT 'generic_id'::ehr.party_ref_id_type
);
ALTER TABLE ehr.party_identified
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.party_identified
    ADD CONSTRAINT party_identified_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.audit_details
    ADD CONSTRAINT audit_details_committer_fkey FOREIGN KEY (committer) REFERENCES ehr.party_identified (id);

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_composer_fkey FOREIGN KEY (composer) REFERENCES ehr.party_identified (id);

ALTER TABLE ONLY ehr.event_context
    ADD CONSTRAINT event_context_facility_fkey FOREIGN KEY (facility) REFERENCES ehr.party_identified (id);

CREATE TABLE ehr.status
(
    id                uuid              DEFAULT ext.uuid_generate_v4()                                                                                                             NOT NULL,
    ehr_id            uuid,
    is_queryable      boolean           DEFAULT true,
    is_modifiable     boolean           DEFAULT true,
    party             uuid                                                                                                                                                         NOT NULL,
    other_details     jsonb,
    sys_transaction   timestamp without time zone                                                                                                                                  NOT NULL,
    sys_period        tstzrange                                                                                                                                                    NOT NULL,
    has_audit         uuid                                                                                                                                                         NOT NULL,
    attestation_ref   uuid,
    in_contribution   uuid                                                                                                                                                         NOT NULL,
    archetype_node_id text              DEFAULT 'openEHR-EHR-EHR_STATUS.generic.v1'::text                                                                                          NOT NULL,
    name              ehr.dv_coded_text DEFAULT ROW ('EHR Status'::text, NULL::ehr.code_phrase, NULL::text, NULL::ehr.code_phrase, NULL::ehr.code_phrase, NULL)::ehr.dv_coded_text NOT NULL
);
ALTER TABLE ehr.status
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.status
    ADD CONSTRAINT status_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.status
    ADD CONSTRAINT status_ehr_id_fkey FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.status
    ADD CONSTRAINT status_has_audit_fkey FOREIGN KEY (has_audit) REFERENCES ehr.audit_details (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.status
    ADD CONSTRAINT status_attestation_ref_fkey FOREIGN KEY (attestation_ref) REFERENCES ehr.attestation_ref (ref) ON DELETE CASCADE;

COMMENT ON TABLE ehr.status IS 'specifies an ehr modality and ownership (patient)';

-- this table is used to link compositions. It is not standard and temporary!
CREATE TABLE ehr.compo_xref
(
    master_uuid     uuid,
    child_uuid      uuid,
    sys_transaction timestamp without time zone NOT NULL
);
ALTER TABLE ehr.compo_xref
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.compo_xref
    ADD CONSTRAINT compo_xref_child_uuid_fkey FOREIGN KEY (child_uuid) REFERENCES ehr.composition (id);

ALTER TABLE ONLY ehr.compo_xref
    ADD CONSTRAINT compo_xref_master_uuid_fkey FOREIGN KEY (master_uuid) REFERENCES ehr.composition (id);

CREATE TABLE ehr.composition_history
(
    id              uuid                        NOT NULL,
    ehr_id          uuid,
    in_contribution uuid,
    active          boolean,
    is_persistent   boolean,
    language        character varying(5),
    territory       integer,
    composer        uuid                        NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL,
    has_audit       uuid,
    attestation_ref uuid,
    feeder_audit    jsonb,
    links           jsonb
);
ALTER TABLE ehr.composition_history
    OWNER TO ehrbase;

CREATE TABLE ehr.concept
(
    id          uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    conceptid   integer,
    language    character varying(5),
    description text
);
ALTER TABLE ehr.concept
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.concept
    ADD CONSTRAINT concept_pkey PRIMARY KEY (id);

COMMENT ON TABLE ehr.concept IS 'openEHR common concepts (e.g. terminology) used in the system';

CREATE TABLE ehr.contribution
(
    id                uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    ehr_id            uuid,
    contribution_type ehr.contribution_data_type,
    state             ehr.contribution_state,
    signature         text,
    has_audit         uuid
);
ALTER TABLE ehr.contribution
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.contribution
    ADD CONSTRAINT contribution_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.contribution
    ADD CONSTRAINT contribution_ehr_id_fkey FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.contribution
    ADD CONSTRAINT contribution_has_audit_fkey FOREIGN KEY (has_audit) REFERENCES ehr.audit_details (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_in_contribution_fkey FOREIGN KEY (in_contribution) REFERENCES ehr.contribution (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.status
    ADD CONSTRAINT status_party_fkey FOREIGN KEY (party) REFERENCES ehr.party_identified (id);

ALTER TABLE ONLY ehr.status
    ADD CONSTRAINT status_in_contribution_fkey FOREIGN KEY (in_contribution) REFERENCES ehr.contribution (id) ON DELETE CASCADE;


COMMENT ON TABLE ehr.contribution IS 'Contribution table, compositions reference this table';

CREATE TABLE ehr.identifier
(
    id_value  text,
    issuer    text,
    assigner  text,
    type_name text,
    party     uuid NOT NULL
);
ALTER TABLE ehr.identifier
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.identifier
    ADD CONSTRAINT identifier_party_fkey FOREIGN KEY (party) REFERENCES ehr.party_identified (id) ON DELETE CASCADE;

COMMENT ON TABLE ehr.identifier IS 'specifies an identifier for a party identified, more than one identifier is possible';

CREATE TABLE ehr.entry_history
(
    id              uuid                        NOT NULL,
    composition_id  uuid,
    sequence        integer,
    item_type       ehr.entry_type,
    template_id     text,
    template_uuid   uuid,
    archetype_id    text,
    category        ehr.dv_coded_text,
    entry           jsonb,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL,
    rm_version      text,
    name            ehr.dv_coded_text
);
ALTER TABLE ehr.entry_history
    OWNER TO ehrbase;

CREATE TABLE ehr.event_context_history
(
    id              uuid                        NOT NULL,
    composition_id  uuid,
    start_time      timestamp without time zone NOT NULL,
    start_time_tzid text,
    end_time        timestamp without time zone,
    end_time_tzid   text,
    facility        uuid,
    location        text,
    other_context   jsonb,
    setting         ehr.dv_coded_text,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL
);
ALTER TABLE ehr.event_context_history
    OWNER TO ehrbase;

CREATE TABLE ehr.folder
(
    id                uuid    DEFAULT ext.uuid_generate_v4() NOT NULL,
    in_contribution   uuid                                   NOT NULL,
    name              text                                   NOT NULL,
    archetype_node_id text                                   NOT NULL,
    active            boolean DEFAULT true,
    details           jsonb,
    sys_transaction   timestamp without time zone            NOT NULL,
    sys_period        tstzrange                              NOT NULL,
    has_audit         uuid                                   NOT NULL
);
ALTER TABLE ehr.folder
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.folder
    ADD CONSTRAINT folder_pk PRIMARY KEY (id);

ALTER TABLE ONLY ehr.folder
    ADD CONSTRAINT folder_has_audit_fkey FOREIGN KEY (has_audit) REFERENCES ehr.audit_details (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.folder
     ADD CONSTRAINT folder_in_contribution_fkey FOREIGN KEY (in_contribution) REFERENCES ehr.contribution (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.ehr
    ADD CONSTRAINT ehr_directory_fkey FOREIGN KEY (directory) REFERENCES ehr.folder (id);

CREATE TABLE ehr.folder_hierarchy
(
    parent_folder   uuid                        NOT NULL,
    child_folder    uuid                        NOT NULL,
    in_contribution uuid,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL
);
ALTER TABLE ehr.folder_hierarchy
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.folder_hierarchy
    ADD CONSTRAINT folder_hierarchy_pkey PRIMARY KEY (parent_folder, child_folder);

ALTER TABLE ONLY ehr.folder_hierarchy
    ADD CONSTRAINT folder_hierarchy_in_contribution_fk FOREIGN KEY (in_contribution) REFERENCES ehr.contribution (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.folder_hierarchy
    ADD CONSTRAINT folder_hierarchy_parent_fk FOREIGN KEY (parent_folder) REFERENCES ehr.folder (id) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE;

ALTER TABLE ONLY ehr.folder_hierarchy
    ADD CONSTRAINT uq_folderhierarchy_parent_child UNIQUE (parent_folder, child_folder);

CREATE TABLE ehr.folder_hierarchy_history
(
    parent_folder   uuid                        NOT NULL,
    child_folder    uuid                        NOT NULL,
    in_contribution uuid                        NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL
);
ALTER TABLE ehr.folder_hierarchy_history
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.folder_hierarchy_history
    ADD CONSTRAINT folder_hierarchy_history_pkey PRIMARY KEY (parent_folder, child_folder, in_contribution);

CREATE TABLE ehr.folder_history
(
    id                uuid                        NOT NULL,
    in_contribution   uuid                        NOT NULL,
    name              text                        NOT NULL,
    archetype_node_id text                        NOT NULL,
    active            boolean                     NOT NULL,
    details           jsonb,
    sys_transaction   timestamp without time zone NOT NULL,
    sys_period        tstzrange                   NOT NULL,
    has_audit         uuid                        NOT NULL
);
ALTER TABLE ehr.folder_history
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.folder_history
    ADD CONSTRAINT folder_history_pkey PRIMARY KEY (id, in_contribution);

CREATE TABLE ehr.folder_items
(
    folder_id       uuid                        NOT NULL,
    object_ref_id   uuid                        NOT NULL,
    in_contribution uuid                        NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL
);
ALTER TABLE ehr.folder_items
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.folder_items
    ADD CONSTRAINT folder_items_pkey PRIMARY KEY (folder_id, object_ref_id, in_contribution);

ALTER TABLE ONLY ehr.folder_items
    ADD CONSTRAINT folder_items_folder_fkey FOREIGN KEY (folder_id) REFERENCES ehr.folder (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.folder_items
    ADD CONSTRAINT folder_items_in_contribution_fkey FOREIGN KEY (in_contribution) REFERENCES ehr.contribution (id);

CREATE TABLE ehr.folder_items_history
(
    folder_id       uuid                        NOT NULL,
    object_ref_id   uuid                        NOT NULL,
    in_contribution uuid                        NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL
);
ALTER TABLE ehr.folder_items_history
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.folder_items_history
    ADD CONSTRAINT folder_items_hist_pkey PRIMARY KEY (folder_id, object_ref_id, in_contribution);

CREATE TABLE ehr.heading
(
    code        character varying(16) NOT NULL,
    name        text,
    description text
);
ALTER TABLE ehr.heading
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.heading
    ADD CONSTRAINT heading_pkey PRIMARY KEY (code);

CREATE TABLE ehr.language
(
    code        character varying(5) NOT NULL,
    description text                 NOT NULL
);
ALTER TABLE ehr.language
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.language
    ADD CONSTRAINT language_pkey PRIMARY KEY (code);

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_language_fkey FOREIGN KEY (language) REFERENCES ehr.language (code);

ALTER TABLE ONLY ehr.concept
    ADD CONSTRAINT concept_language_fkey FOREIGN KEY (language) REFERENCES ehr.language (code);

COMMENT ON TABLE ehr.language IS 'ISO 639-1 language codeset';

CREATE TABLE ehr.object_ref
(
    id_namespace    text                        NOT NULL,
    type            text                        NOT NULL,
    id              uuid                        NOT NULL,
    in_contribution uuid                        NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL
);
ALTER TABLE ehr.object_ref
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.object_ref
    ADD CONSTRAINT object_ref_pkey PRIMARY KEY (id, in_contribution);

ALTER TABLE ONLY ehr.object_ref
    ADD CONSTRAINT object_ref_in_contribution_fkey FOREIGN KEY (in_contribution) REFERENCES ehr.contribution (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.folder_items
    ADD CONSTRAINT folder_items_obj_ref_fkey FOREIGN KEY (in_contribution, object_ref_id) REFERENCES ehr.object_ref (in_contribution, id) ON DELETE CASCADE;


COMMENT ON TABLE ehr.object_ref IS '*implements https://specifications.openehr.org/releases/RM/Release-1.0.3/support.html#_object_ref_class*id implemented as native UID from postgres instead of a separate table.';

CREATE TABLE ehr.object_ref_history
(
    id_namespace    text                        NOT NULL,
    type            text                        NOT NULL,
    id              uuid                        NOT NULL,
    in_contribution uuid                        NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL
);
ALTER TABLE ehr.object_ref_history
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.object_ref_history
    ADD CONSTRAINT object_ref_hist_pkey PRIMARY KEY (id, in_contribution);

COMMENT ON TABLE ehr.object_ref_history IS '*implements https://specifications.openehr.org/releases/RM/Release-1.0.3/support.html#_object_ref_history_class*id implemented as native UID from postgres instead of a separate table.';

CREATE TABLE ehr.participation
(
    id              uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    event_context   uuid                                NOT NULL,
    performer       uuid,
    function        ehr.dv_coded_text,
    mode            ehr.dv_coded_text,
    time_lower      timestamp without time zone,
    time_lower_tz   text,
    sys_transaction timestamp without time zone         NOT NULL,
    sys_period      tstzrange                           NOT NULL,
    time_upper      timestamp without time zone,
    time_upper_tz   text
);
ALTER TABLE ehr.participation
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.participation
    ADD CONSTRAINT participation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.participation
    ADD CONSTRAINT participation_event_context_fkey FOREIGN KEY (event_context) REFERENCES ehr.event_context (id) ON DELETE CASCADE;

ALTER TABLE ONLY ehr.participation
    ADD CONSTRAINT participation_performer_fkey FOREIGN KEY (performer) REFERENCES ehr.party_identified (id);

COMMENT ON TABLE ehr.participation IS 'define a participating party for an event f.ex.';

CREATE TABLE ehr.participation_history
(
    id              uuid                        NOT NULL,
    event_context   uuid                        NOT NULL,
    performer       uuid,
    function        ehr.dv_coded_text,
    mode            ehr.dv_coded_text,
    time_lower      timestamp without time zone,
    time_lower_tz   text,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period      tstzrange                   NOT NULL,
    time_upper      timestamp without time zone,
    time_upper_tz   text
);
ALTER TABLE ehr.participation_history
    OWNER TO ehrbase;

CREATE TABLE ehr.session_log
(
    id           uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    subject_id   text                                NOT NULL,
    node_id      text,
    session_id   text,
    session_name text,
    session_time timestamp without time zone,
    ip_address   text
);
ALTER TABLE ehr.session_log
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.session_log
    ADD CONSTRAINT session_log_pkey PRIMARY KEY (id);

CREATE TABLE ehr.status_history
(
    id                uuid                                                                                                                                                         NOT NULL,
    ehr_id            uuid,
    is_queryable      boolean,
    is_modifiable     boolean,
    party             uuid                                                                                                                                                         NOT NULL,
    other_details     jsonb,
    sys_transaction   timestamp without time zone                                                                                                                                  NOT NULL,
    sys_period        tstzrange                                                                                                                                                    NOT NULL,
    has_audit         uuid                                                                                                                                                         NOT NULL,
    attestation_ref   uuid,
    in_contribution   uuid                                                                                                                                                         NOT NULL,
    archetype_node_id text              DEFAULT 'openEHR-EHR-EHR_STATUS.generic.v1'::text                                                                                          NOT NULL,
    name              ehr.dv_coded_text DEFAULT ROW ('EHR Status'::text, NULL::ehr.code_phrase, NULL::text, NULL::ehr.code_phrase, NULL::ehr.code_phrase, NULL)::ehr.dv_coded_text NOT NULL
);
ALTER TABLE ehr.status_history
    OWNER TO ehrbase;

CREATE TABLE ehr.stored_query
(
    reverse_domain_name character varying                                              NOT NULL,
    semantic_id         character varying                                              NOT NULL,
    semver              character varying           DEFAULT '0.0.0'::character varying NOT NULL,
    query_text          character varying                                              NOT NULL,
    creation_date       timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    type                character varying           DEFAULT 'AQL'::character varying,
    CONSTRAINT stored_query_reverse_domain_name_check CHECK (((reverse_domain_name)::text ~*
                                                              '^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$'::text)),
    CONSTRAINT stored_query_semantic_id_check CHECK (((semantic_id)::text ~* '[\w|\-|_|]+'::text)),
    CONSTRAINT stored_query_semver_check CHECK (((semver)::text ~*
                                                 '^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$'::text))
);
ALTER TABLE ehr.stored_query
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.stored_query
    ADD CONSTRAINT pk_qualified_name PRIMARY KEY (reverse_domain_name, semantic_id, semver);

CREATE TABLE ehr.system
(
    id          uuid DEFAULT ext.uuid_generate_v4() NOT NULL,
    description text                                NOT NULL,
    settings    text                                NOT NULL
);
ALTER TABLE ehr.system
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.system
    ADD CONSTRAINT system_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ehr.audit_details
    ADD CONSTRAINT audit_details_system_id_fkey FOREIGN KEY (system_id) REFERENCES ehr.system (id);

ALTER TABLE ONLY ehr.ehr
    ADD CONSTRAINT ehr_system_id_fkey FOREIGN KEY (system_id) REFERENCES ehr.system (id);

COMMENT ON TABLE ehr.system IS 'system table for reference';

CREATE TABLE ehr.template_store
(
    id              uuid                        NOT NULL,
    template_id     text,
    content         text,
    sys_transaction timestamp without time zone NOT NULL
);
ALTER TABLE ehr.template_store
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.template_store
    ADD CONSTRAINT template_store_pkey PRIMARY KEY (id);

CREATE TABLE ehr.terminology_provider
(
    code      text NOT NULL,
    source    text NOT NULL,
    authority text
);
ALTER TABLE ehr.terminology_provider
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.terminology_provider
    ADD CONSTRAINT terminology_provider_pkey PRIMARY KEY (code);

COMMENT ON TABLE ehr.terminology_provider IS 'openEHR identified terminology provider';

CREATE TABLE ehr.territory
(
    code        integer NOT NULL,
    twoletter   character(2),
    threeletter character(3),
    text        text    NOT NULL
);
ALTER TABLE ehr.territory
    OWNER TO ehrbase;

ALTER TABLE ONLY ehr.territory
    ADD CONSTRAINT territory_pkey PRIMARY KEY (code);

ALTER TABLE ONLY ehr.composition
    ADD CONSTRAINT composition_territory_fkey FOREIGN KEY (territory) REFERENCES ehr.territory (code);

COMMENT ON TABLE ehr.territory IS 'ISO 3166-1 countries codeset';

-- TRIGGERS and INDEXES
CREATE INDEX attestation_reference_idx ON ehr.attestation USING btree (reference);
CREATE INDEX attested_view_attestation_idx ON ehr.attested_view USING btree (attestation_id);
CREATE INDEX compo_xref_child_idx ON ehr.compo_xref USING btree (child_uuid);
CREATE INDEX composition_composer_idx ON ehr.composition USING btree (composer);
CREATE INDEX composition_ehr_idx ON ehr.composition USING btree (ehr_id);
CREATE INDEX composition_history_ehr_idx ON ehr.composition_history USING btree (ehr_id);
CREATE INDEX context_composition_id_idx ON ehr.event_context USING btree (composition_id);
CREATE INDEX context_facility_idx ON ehr.event_context USING btree (facility);
CREATE INDEX context_participation_index ON ehr.participation USING btree (event_context);
-- CREATE INDEX context_setting_idx ON ehr.event_context USING btree (setting);
CREATE INDEX contribution_ehr_idx ON ehr.contribution USING btree (ehr_id);
CREATE INDEX ehr_compo_xref ON ehr.compo_xref USING btree (master_uuid);
CREATE INDEX ehr_composition_history ON ehr.composition_history USING btree (id);
CREATE INDEX ehr_entry_history ON ehr.entry_history USING btree (id);
CREATE INDEX ehr_event_context_history ON ehr.event_context_history USING btree (id);
CREATE UNIQUE INDEX ehr_folder_idx ON ehr.ehr USING btree (directory);
CREATE INDEX ehr_participation_history ON ehr.participation_history USING btree (id);
CREATE INDEX ehr_status_history ON ehr.status_history USING btree (id);

CREATE INDEX ehr_subject_id_index ON ehr.party_identified USING btree (jsonb_extract_path_text(
                                                                               (ehr.js_party_ref(party_ref_value,
                                                                                                 party_ref_scheme,
                                                                                                 party_ref_namespace,
                                                                                                 party_ref_type))::jsonb,
                                                                               VARIADIC
                                                                               ARRAY ['id'::text, 'value'::text]));

CREATE INDEX entry_composition_id_idx ON ehr.entry USING btree (composition_id);
CREATE INDEX entry_history_composition_idx ON ehr.entry_history USING btree (composition_id);
CREATE INDEX event_context_history_composition_idx ON ehr.event_context_history USING btree (composition_id);
CREATE INDEX fki_folder_hierarchy_parent_fk ON ehr.folder_hierarchy USING btree (parent_folder);
CREATE INDEX folder_hierarchy_history_contribution_idx ON ehr.folder_hierarchy_history USING btree (in_contribution);
CREATE INDEX folder_hierarchy_in_contribution_idx ON ehr.folder_hierarchy USING btree (in_contribution);
CREATE INDEX folder_hist_idx ON ehr.folder_items_history USING btree (folder_id, object_ref_id, in_contribution);
CREATE INDEX folder_history_contribution_idx ON ehr.folder_history USING btree (in_contribution);
CREATE INDEX folder_in_contribution_idx ON ehr.folder USING btree (in_contribution);
CREATE INDEX folder_items_contribution_idx ON ehr.folder_items USING btree (in_contribution);
CREATE INDEX folder_items_history_contribution_idx ON ehr.folder_items_history USING btree (in_contribution);
CREATE INDEX gin_entry_path_idx ON ehr.entry USING gin (entry jsonb_path_ops);
CREATE INDEX obj_ref_in_contribution_idx ON ehr.object_ref USING btree (in_contribution);
CREATE INDEX object_ref_history_contribution_idx ON ehr.object_ref_history USING btree (in_contribution);
CREATE INDEX participation_history_event_context_idx ON ehr.participation_history USING btree (event_context);
CREATE INDEX party_identified_party_ref_idx ON ehr.party_identified USING btree (party_ref_namespace, party_ref_scheme, party_ref_value);
CREATE INDEX party_identified_party_type_idx ON ehr.party_identified USING btree (party_type, name);
CREATE INDEX status_ehr_idx ON ehr.status USING btree (ehr_id);
CREATE INDEX status_history_ehr_idx ON ehr.status_history USING btree (ehr_id);
CREATE INDEX status_party_idx ON ehr.status USING btree (party);
CREATE INDEX template_entry_idx ON ehr.entry USING btree (template_id);
CREATE UNIQUE INDEX territory_code_index ON ehr.territory USING btree (code);

-- TRIGGERS

CREATE TRIGGER tr_folder_item_delete
    AFTER DELETE
    ON ehr.folder_items
    FOR EACH ROW
EXECUTE FUNCTION ehr.tr_function_delete_folder_item();
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.composition
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.composition_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.entry
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.entry_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.event_context
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.event_context_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.folder
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.folder_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.folder_hierarchy
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.folder_hierarchy_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.folder_items
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.folder_items_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.object_ref
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.object_ref_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.participation
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.participation_history', 'true');
CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.status
    FOR EACH ROW
EXECUTE FUNCTION ext.versioning('sys_period', 'ehr.status_history', 'true');

-- BUILT-IN
GRANT ALL ON FUNCTION ext.uuid_generate_v1() TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_generate_v1mc() TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_generate_v3(namespace uuid, name text) TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_generate_v4() TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_generate_v5(namespace uuid, name text) TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_nil() TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_ns_dns() TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_ns_oid() TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_ns_url() TO ehrbase;
GRANT ALL ON FUNCTION ext.uuid_ns_x500() TO ehrbase;
