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

CREATE INDEX party_identified_party_type_idx ON ehr.party_identified(party_type, name);

CREATE INDEX party_identified_party_ref_idx ON ehr.party_identified(party_ref_namespace, party_ref_scheme, party_ref_value);