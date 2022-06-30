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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/EHR_COMPOSITION.md#b6d-alternative-flow-3-create-new-invalid-event-composition
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot

Suite Setup     Precondition
#Suite Teardown  restart SUT


*** Test Cases ***
Alternative flow 3 create new invalid event COMPOSITION RAW_JSON
    commit composition   format=CANONICAL_JSON
    ...                  composition=nested.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION RAW_XML
    commit composition   format=CANONICAL_XML
    ...                  composition=nested.en.v1__invalid_wrong_structure.xml
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION FLAT
    [Tags]
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION FLAT - DV Duration Near To Max
    [Tags]      not-ready   bug
    commit composition   format=FLAT
    ...                  composition=dv_duration_max__.json
    #Github issue: https://github.com/ehrbase/ehrbase/issues/926
    check status_code of commit composition    400
    [Teardown]      TRACE JIRA ISSUE    CDR-447

Alternative flow 3 create new invalid event COMPOSITION TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=nested.en.v1__invalid_wrong_structure.xml
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=nested.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

*** Keywords ***
Precondition
    Upload OPT      nested/nested.opt
    Upload OPT      all_types/dv_duration_max.opt
    create EHR