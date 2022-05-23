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
Resource        ../../_resources/keywords/aql_query_keywords.robot

Suite Setup       Precondition
Suite Teardown    restart SUT

*** Test Cases ***
Main flow has existing COMPOSITION (FLAT)
    create EHR
    commit composition   format=FLAT
    ...                  composition=ehrn_vital_signs.v2__.json
    check the successful result of commit composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    check composition exists

Create Two Compositions With Health Care Facility Provided And Not Provided - AQL
    [Documentation]     Create first composition with health_care_facility provided;
    ...     Create second composition with health_care_facility not provided;
    ...     Apply AQL query to get EHR a/uid/value and a/context/health_care_facility/name.
    ...     In rows, first array should contain Hospital A and second array should contain null.
    ...     - https://github.com/ehrbase/ehrbase/issues/848
    ...     Second query checks the resulted columns data.
    ...     - https://github.com/ehrbase/ehrbase/issues/787
    create EHR
    commit composition   format=FLAT
    ...                  composition=minimal_action.en.v1__health_care_facility_select_populated.json
    check the successful result of commit composition
    commit composition   format=FLAT
    ...                  composition=minimal_action.en.v1__health_care_facility_select_missing.json
    check the successful result of commit composition

    ${query1}       Catenate
    ...     SELECT a/uid/value as composition_uid,
    ...     a/context/health_care_facility/name as healthcare_facility_name
    ...     FROM EHR e contains COMPOSITION
    ...     a contains ACTION a0[openEHR-EHR-ACTION.minimal.v1]
    Set Test Variable    ${payload}    {"q": "${query1}"}
    POST /query/aql (REST)     JSON
    Should Be Equal As Strings     ${response body["rows"][0][1]}   Hospital A
    Should Be Equal     ${response body["rows"][1][1]}      ${None}
    ## Cover issue: https://github.com/ehrbase/ehrbase/issues/787
    ${query2}       Catenate
    ...     SELECT c as COMPOSITION
    ...     FROM EHR e
    ...     CONTAINS composition c
    Set Test Variable    ${payload}    {"q": "${query2}"}
    POST /query/aql (REST)     JSON
    Should Be Equal As Strings     ${response body["columns"][0]["path"]}   c
    Should Be Equal As Strings     ${response body["columns"][0]["name"]}   COMPOSITION

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
    Upload OPT    minimal/minimal_action.opt
    create EHR