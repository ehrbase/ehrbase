# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School),
# Nataliya Flusman (Solit Clouds), Nikita Danilin (Solit Clouds)
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Documentation   Composition Integration Tests
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/EHR_COMPOSITION.md#b6g-alternative-flow-6-create-new-event-composition-ehr-doesnt-exist
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot

Suite Setup     Precondition


*** Test Cases ***
Alternative flow 6 create new event COMPOSITION EHR doesnt exist CANONICAL_JSON
    commit composition   format=CANONICAL_JSON
    ...                  composition=nested.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist CANONICAL_XML
    commit composition   format=CANONICAL_XML
    ...                  composition=nested.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist FLAT
    [Tags]
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.xml.flat.json
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=nested.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=nested.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist CANONICAL_JSON
    commit composition   format=CANONICAL_JSON
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist CANONICAL_XML
    commit composition   format=CANONICAL_XML
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist FLAT
    [Tags]
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__full.xml.flat.json
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition    404


*** Keywords ***
Precondition
    Upload OPT    nested/nested.opt
    Upload OPT    minimal_persistent/persistent_minimal.opt
    create fake EHR