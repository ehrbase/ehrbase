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

create index identifier_value_idx on ehr.identifier (id_value);
create index identifier_type_name_idx on ehr.identifier (type_name);

do
$$
    declare
        duplicate_user record;
    begin
        select i.id_value
        into duplicate_user
        from ehr.identifier i
        where i.type_name = 'EHRbase Security Authentication User'
        group by i.id_value
        having count(i.id_value) > 1;

        if found then
            raise exception 'Your database contains duplicate users. Please refer to updating EHRbase procedure (see UPDATING.md) and/or https://github.com/ehrbase/ehrbase/pull/826';
        end if;
    end
$$;
