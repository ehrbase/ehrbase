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

do
$$
    declare
        duplicate_user record;
        party_id       uuid;
    begin
        for duplicate_user in (select i.id_value, min(ctid) as ctid
                               from ehr.identifier i
                               where i.type_name = 'EHRbase Security Authentication User'
                               group by i.id_value
                               having count(i.id_value) > 1)
            loop
                select party
                into party_id
                from ehr.identifier
                where id_value = duplicate_user.id_value
                  and ctid = duplicate_user.ctid;

                update ehr.audit_details
                set committer = party_id
                from ehr.party_identified pi
                         inner join ehr.identifier i
                                    on i.party = pi.id
                where committer = pi.id
                  and i.id_value = duplicate_user.id_value;


                delete
                from ehr.party_identified pi
                where pi.id IN
                      (select i.party
                       from ehr.identifier i
                       where i.id_value = duplicate_user.id_value
                         and i.ctid <> duplicate_user.ctid);
            end loop;
    end
$$;
