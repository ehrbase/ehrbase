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
Documentation   AQL Integration Tests
...            Precondtions:
...              1. operational_templates folder is empty
...              2. CLEAN DB container started !!!
...              3. openehr-server (jar) started

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/aql_query_keywords.robot
# Resource    ${CURDIR}${/}../_resources/keywords/composition_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Test Setup  start openehr server
# Test Teardown  restore clean SUT state

Force Tags      AQL    obsolete



*** Test Cases ***
Establish Preconditions: load valid OPTs into SUT
    [Template]    upload valid OPT

    minimal/minimal_action.opt
    minimal/minimal_observation.opt
    minimal/minimal_evaluation.opt
    minimal/minimal_admin.opt
    minimal/minimal_instruction.opt
    all_types/Test_all_types.opt

    [Teardown]    abort test execution if this test fails


AQL query EHR records by ehrId
    [Tags]   not-ready

    create EHR XML
    execute AQL query          {"aql" : "select a/uid/value as uid, a/composer/name as author, a/context/start_time/value as date_created from EHR e [ehr_id/value = '${ehr_id}'] contains COMPOSITION a[openEHR-EHR-COMPOSITION.health_summary.v1]"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=0    maxItems=0


AQL get ehr_id from all EHRs
    [Documentation]            ...
    create EHR XML
    execute AQL query          {"aql": "SELECT e/ehr_id/value as uid FROM EHR e"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=2    maxItems=2


AQL get ehr_id from EHR with given ehr_id
    create EHR XML
    execute AQL query          {"aql": "SELECT e/ehr_id/value as uid FROM EHR e [ehr_id/value='${ehr_id}']"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1

    # [Teardown]    restart ehrdb


AQL get ehr_id from EHR with given ehr_id using WHERE
    create EHR XML
    execute AQL query           {"aql": "SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value = '${ehr_id}'"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1

    # [Teardown]    restart ehrdb


AQL get ehr_id from all EHRs CONTAINS COMPO
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_observation.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    # maxItems=1
                                                      # maxItems could be anything here because the ehr_id is not constrainted in the query

    [Teardown]    restart ehrdb


AQL get ehr_id from all EHRs CONTAINS COMPO with arch_id
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_observation.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1]"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1

    [Teardown]    restart ehrdb


AQL get ehr_id from all EHRs CONTAINS COMPO with arch_id using WHERE
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_observation.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c WHERE c/archetype_node_id='openEHR-EHR-COMPOSITION.minimal.v1'"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1

    [Teardown]    restart ehrdb


AQL get ehr_id from all EHRs CONTAINS COMPO with not existing arch_id
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_observation.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.xxxxx.v1]"}

    # verify query result
    Output   response body
    Array    response body resultSet    maxItems=0

    # [Teardown]    restart ehrdb


AQL for committed data OBSERVATION
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_observation.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT c/uid/value, c/archetype_node_id, c/archetype_details/template_id/value, o/data[at0001]/origin/value as origin_value, o/data[at0001]/events[at0002]/time/value, o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1]"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1


AQL for committed data EVALUATION
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_evaluation.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT c/uid/value, c/archetype_node_id, c/archetype_details/template_id/value, ev/data[at0001]/items[at0002]/value/magnitude as qty_magnitude, ev/data[at0001]/items[at0002]/value/units as qty_units FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS EVALUATION ev [openEHR-EHR-EVALUATION.minimal.v1]"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1


AQL for committed data ACTION
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_action.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT c/uid/value, c/archetype_node_id, c/archetype_details/template_id/value, a/time/value as action_time, a/ism_transition/current_state/value as current_state, a/description[at0001]/items[at0002]/value/uri/value as mm_uri, a/description[at0001]/items[at0002]/value/size as mm_size FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ACTION a [openEHR-EHR-ACTION.minimal.v1]"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1


AQL for committed data ADMIN_ENTRY
    [Tags]   not-ready

    create EHR XML
    commit composition (XML)    minimal_admin.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT c/uid/value, c/archetype_node_id, c/archetype_details/template_id/value, a/time/value as action_time, a/ism_transition/current_state/value as current_state, a/description[at0001]/items[at0002]/value/uri/value as mm_uri, a/description[at0001]/items[at0002]/value/size as mm_size FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ADMIN_ENTRY a [openEHR-EHR-ADMIN_ENTRY.minimal.v1]"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1


AQL for committed data OBSERVATION WHERE =
    [Tags]    not-ready

    create EHR XML
    commit composition (XML)    minimal_observation.en.v1.instance_xml_input_1.xml
    execute AQL query           {"aql": "SELECT c/uid/value, c/archetype_node_id, c/archetype_details/template_id/value, o/data[at0001]/origin/value as origin_value, o/data[at0001]/events[at0002]/time/value, o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1] WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'first value'"}

    # verify query result
    Output   response body
    Array    response body resultSet    minItems=1    maxItems=1



*** Keywords ***
upload valid OPT
    [Arguments]             ${opt file}

    template_opt1.4_keywords.start request session
    get valid OPT file      ${opt file}
    extract template_id from OPT file
    upload OPT file
    server accepted OPT


commit composition (XML)
    [Arguments]         ${xml_composition}
    [Documentation]     Creates a composition by using POST method and XML file
    ...                 from `/test_data_sets/xml_compositions/` folder
    ...                 DEPENDENCY: use it right after `create EHR XML` which
    ...                             provides the `ehr_id`.


                                        # TODO: FIX PATH!!!!
    ${file}=            Get File           ${CURDIR}/../_resources/test_data_sets/xml_compositions/${xml_composition}
    &{headers}=         Create Dictionary  Content-Type=application/xml  Prefer=return=representation  Accept=application/xml
    ${resp}=            Post Request       ${SUT}   /ehr/${ehr_id}/composition   data=${file}   headers=${headers}
                        Should Be Equal As Strings   ${resp.status_code}   201

    ${xresp}=           Parse Xml          ${resp.text}
                        Log Element        ${xresp}
                        Log Element        ${xresp}  xpath=composition
    ${xcompo_version_uid}=     Get Element        ${xresp}      composition/uid/value
                        Set Test Variable  ${compo_version_uid}     ${xcompo_version_uid.text}
                        # Log To Console     ${compo_version_uid}
