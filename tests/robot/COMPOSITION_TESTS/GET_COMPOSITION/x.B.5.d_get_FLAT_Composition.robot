# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
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
Documentation       Composition Integration Tests
Metadata            TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot

Force Tags

Suite Setup       Precondition
Suite Teardown    restart SUT

*** Test Cases ***
Main flow has existing COMPOSITION (FLAT)
    [Tags]
    upload OPT    all_types/ehrn_vital_signs.v2.opt
    create EHR
    commit composition   format=FLAT
    ...                  composition=ehrn_vital_signs.v2__.json
    check the successful result of commit composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    check composition exists



Data driven tests for Compare content of compositions with the Original (FLAT)
    [Tags]  600  not-ready  bug
    [Template]    Create and compare content of flat compositions

    #flat_composition_file_name
    ehrn_vital_signs.v2__.json
    nested.en.v1__full.xml.flat.json

    TRACE GITHUB ISSUE  600  bug


*** Keywords ***
Create and compare content of flat compositions
    [Arguments]    ${flat_composition_file_name}
    commit composition   format=FLAT
    ...                  composition=${flat_composition_file_name}
    check the successful result of commit composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    check composition exists
    Compare content of compositions with the Original (FLAT)  ${COMPO DATA SETS}/FLAT/${flat_composition_file_name}

Precondition
    Upload OPT    nested/nested.opt
    Upload OPT    all_types/ehrn_vital_signs.v2.opt
    create EHR