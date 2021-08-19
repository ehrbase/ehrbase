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

