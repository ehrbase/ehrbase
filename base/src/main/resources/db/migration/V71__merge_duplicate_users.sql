--
-- Copyright 2022 vitasystems GmbH and Hannover Medical School.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

create temporary table temp_replacements as
with user_identifier as (
    select *
    from ehr.identifier i
    where i.type_name = 'EHRbase Security Authentication User'
),
duplicate_user as (
	select i.id_value,
	       min(party::varchar)::uuid as replacement
    from user_identifier i
    group by i.id_value
    having count(i.id_value) > 1
)
select i.id_value,
       i.party,
       d.replacement
from user_identifier i
join duplicate_user d on (d.id_value = i.id_value and d.replacement != i.party);

update ehr.audit_details
set committer = r.replacement
from temp_replacements r
where r.party = committer;

delete
from ehr.identifier pi
where pi.party IN
      (select r.party
       from temp_replacements r);

--temporarily remove foreign keys
alter table ehr.audit_details drop constraint audit_details_committer_fkey;
alter table ehr.identifier drop constraint identifier_party_fkey;
alter table ehr.composition drop constraint composition_composer_fkey;
alter table ehr.event_context drop constraint event_context_facility_fkey;
alter table ehr.participation drop constraint participation_performer_fkey;
alter table ehr.status drop constraint status_party_fkey;

delete
from ehr.party_identified pi
where pi.id IN
      (select r.party
       from temp_replacements r);

drop table temp_replacements;

create index identifier_value_idx on ehr.identifier (id_value);

--reinstate foreign keys
alter table ehr.audit_details add constraint audit_details_committer_fkey
    foreign key (committer) references ehr.party_identified (id);
alter table ehr.identifier add constraint identifier_party_fkey
    foreign key (party) references ehr.party_identified (id)
        on delete cascade;
alter table ehr.composition add constraint composition_composer_fkey
    foreign key (composer) references ehr.party_identified (id);
alter table ehr.event_context add constraint event_context_facility_fkey
    foreign key (facility) references ehr.party_identified (id);
alter table ehr.participation add constraint participation_performer_fkey
    foreign key (performer) references ehr.party_identified (id);
alter table ehr.status add constraint status_party_fkey
    foreign key (party) references ehr.party_identified (id);