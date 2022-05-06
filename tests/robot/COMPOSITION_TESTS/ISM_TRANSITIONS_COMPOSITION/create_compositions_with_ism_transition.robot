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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/EHR_COMPOSITION.md#b6a-main-flow-create-new-event-composition
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot

Suite Setup       Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Create Composition With Valid ISM Transition
    [Documentation]     Create Composition with valid ISM Transition.
    ...     Positive flow.
    [Tags]  Positive
    commit composition   format=FLAT
    ...                  composition=test-ism.vitagroup.de.v1__.json
    check the successful result of commit composition   nesting

Create Composition With Invalid Current State On ISM Transition
    [Documentation]     Create Composition with
    ...     invalid current state on ISM transition.
    ...     Negative flow.
    [Tags]  Negative    InvalidCurrentState
    commit composition   format=FLAT
    ...                  composition=test-ism.vitagroup.de.v1__ism_invalid_current_state.json
    check status_code of commit composition    400
    Should Contain      ${response.json()["message"]}
    ...     No valid transition found for ism transition[cancel/openehr/166]
    Should Contain      ${response.json()["message"]}
    ...     /content[openEHR-EHR-ACTION.medication.v1]/ism_transition:
    Should Contain      ${response.json()["message"]}
    ...     IsmTransition contains invalid current_state

Create Composition With ISM Missing Transition
    [Documentation]     Create Composition with
    ...     ISM missing transition.
    ...     Positive flow.
    [Tags]  Positive    ISMMissingTransition
    commit composition   format=FLAT
    ...                  composition=test-ism.vitagroup.de.v1__ism_missing_transition.json
    check the successful result of commit composition   nesting

Create Composition With ISM Wrong Current State
    [Documentation]     Create Composition with
    ...     ISM wrong current state.
    ...     Negative flow.
    [Tags]  Negative    ISMMWrongCurrentState
    commit composition   format=FLAT
    ...                  composition=test-ism.vitagroup.de.v1__ism_wrong_current_state.json
    check status_code of commit composition    400
    Should Contain      ${response.json()["message"]}
    ...     No valid transition found for ism transition[cancel/openehr/166]
    Should Contain      ${response.json()["message"]}
    ...     /content[openEHR-EHR-ACTION.medication.v1]/ism_transition:
    Should Contain      ${response.json()["message"]}
    ...     IsmTransition contains invalid current_state


*** Keywords ***
Precondition
    Upload OPT    all_types/test-ism.vitagroup.de.v1.opt
    create EHR
