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

CREATE TABLE ehr.users
(
    username        text,
    party_id  uuid not null REFERENCES ehr.party_identified (id),
    namespace TEXT default '1f332a66-0e57-11ed-861d-0242ac120002',
    PRIMARY KEY (username, namespace)
);

ALTER TABLE ehr.identifier
    disable row level security;

insert into ehr.users
SELECT id_value, party, namespace
from ehr.identifier
where type_name = 'EHRbase Security Authentication User'
  and issuer = 'EHRbase'
  and assigner = 'EHRbase';

ALTER TABLE ehr.identifier
    enable row level security;

ALTER TABLE ehr.users
    ENABLE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr.users FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));