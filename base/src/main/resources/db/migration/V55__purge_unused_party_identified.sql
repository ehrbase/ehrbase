/*
 *  Copyright (c) 2021 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
-- returns the count of occurrences of a party_identified accross table having it as argument
CREATE OR REPLACE FUNCTION ehr.party_usage(party_uuid UUID)
  RETURNS BIGINT AS
$$
BEGIN
	RETURN (
		with usage_uuid as (
			SELECT facility as uuid from ehr.event_context where facility = party_uuid
		UNION
			SELECT facility  as uuid from ehr.event_context_history where facility = party_uuid
		UNION
			SELECT composer  as uuid from ehr.composition where composer = party_uuid
		UNION
			SELECT composer  as uuid from ehr.composition_history where composer = party_uuid
		UNION
			SELECT performer  as uuid from ehr.participation where performer = party_uuid
		UNION
			SELECT performer  as uuid from ehr.participation_history where performer = party_uuid
		UNION
			SELECT party  as uuid from ehr.status where party = party_uuid
		UNION
			SELECT party  as uuid from ehr.status_history where party = party_uuid
        UNION
            SELECT committer as uuid from ehr.audit_details where committer = party_uuid
		)
		SELECT count(usage_uuid.uuid)
			FROM usage_uuid
	);
END
$$
LANGUAGE plpgsql;

-- use this function for debugging purpose
-- identifies where the party_identified is referenced
CREATE OR REPLACE FUNCTION ehr.party_usage_identification(party_uuid UUID)
  RETURNS table(id UUID, entity TEXT) AS
$$
		with usage_uuid as (
			SELECT facility as uuid, 'FACILITY' as entity from ehr.event_context where facility = party_uuid
		UNION
			SELECT facility  as uuid, 'FACILITY_HISTORY' as entity  from ehr.event_context_history where facility = party_uuid
		UNION
			SELECT composer  as uuid, 'COMPOSER' as entity  from ehr.composition where composer = party_uuid
		UNION
			SELECT composer  as uuid, 'COMPOSER_HISTORY' as entity from ehr.composition_history where composer = party_uuid
		UNION
			SELECT performer  as uuid, 'PERFORMER' as entity from ehr.participation where performer = party_uuid
		UNION
			SELECT performer  as uuid, 'PERFORMER_HISTORY' as entity from ehr.participation_history where performer = party_uuid
		UNION
			SELECT party  as uuid, 'SUBJECT' as entity from ehr.status where party = party_uuid
		UNION
			SELECT party  as uuid, 'SUBJECT_HISTORY' as entity from ehr.status_history where party = party_uuid
        UNION
            SELECT committer  as uuid, 'AUDIT_DETAILS' as entity from ehr.audit_details where committer = party_uuid
		)
		SELECT usage_uuid.uuid, usage_uuid.entity
			FROM usage_uuid;
$$
LANGUAGE sql;

-- alter table identifier to add the missing on delete...cascade
alter table ehr.identifier
drop constraint identifier_party_fkey,
add constraint identifier_party_fkey
   foreign key (party)
   references ehr.party_identified(id)
   on delete cascade;

-- garbage collection: delete all party_identified where usage count is 0
--	DELETE FROM party_identified WHERE  ehr.party_usage(party_identified.id) = 0;

-- MODIFICATION of existing function: fixes deletion of participation_history and event_context_history
-- ====================================================================
-- Description: Function to delete event_contexts and participations for a composition and return their parties (event_context.facility and participation.performer).
-- Parameters:
--    @compo_id_input - UUID of super composition
-- Returns: '1' and linked party UUID
-- Requires: Afterwards deletion of returned party.
-- =====================================================================
CREATE OR REPLACE FUNCTION ehr.admin_delete_event_context_for_compo(compo_id_input UUID)
    RETURNS TABLE (num integer, party UUID) AS $$
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
$$ LANGUAGE plpgsql
    RETURNS NULL ON NULL INPUT;

-- delete remaining history records from deleted parents
CREATE OR REPLACE FUNCTION ehr.delete_orphan_history()
  RETURNS BOOLEAN AS
$$
	WITH
		delete_orphan_compo_history as (
			delete from ehr.composition_history where not exists(select 1 from ehr.composition where id = ehr.composition_history.id)
		),
		delete_orphan_event_context_history as (
			delete from ehr.event_context_history where not exists(select 1 from ehr.event_context where event_context.composition_id = ehr.event_context_history.composition_id)
		),
		delete_orphan_participation_history as (
			delete from ehr.participation_history where not exists(select 1 from ehr.participation where participation.event_context = ehr.participation_history.event_context)
		),
        delete_orphan_entry_history as (
            delete from ehr.entry_history where not exists(select 1 from ehr.composition where composition.id = ehr.entry_history.composition_id)
        ),
		delete_orphan_party_identified as (
			DELETE FROM ehr.party_identified WHERE  ehr.party_usage(party_identified.id) = 0
		)
	select true;
$$
LANGUAGE sql;