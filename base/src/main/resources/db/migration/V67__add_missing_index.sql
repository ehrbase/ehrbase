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

CREATE INDEX ehr_concept_id_language_idx ON ehr.concept(conceptid, language);
CREATE INDEX ehr_identifier_party_idx ON ehr.identifier(party);

CREATE UNIQUE INDEX ehr_territory_twoletter_idx ON ehr.territory(twoletter);
CREATE UNIQUE INDEX ehr_system_settings_idx ON ehr.system(settings);

DROP INDEX entry_composition_id_idx;
CREATE UNIQUE INDEX entry_composition_id_idx on ehr.entry(composition_id);