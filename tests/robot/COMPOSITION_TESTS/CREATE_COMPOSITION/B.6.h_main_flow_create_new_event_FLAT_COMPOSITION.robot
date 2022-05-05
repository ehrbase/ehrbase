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
Main flow create new event COMPOSITION FLAT
    [Tags]
    commit composition   format=FLAT
    ...                  composition=ehrn_vital_signs.v2__.json
    check the successful result of commit composition


*** Keywords ***
Precondition
    Upload OPT    all_types/ehrn_vital_signs.v2.opt
    create EHR