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
# Author: Vladislav Ploaia


*** Settings ***
Documentation       EHRScape Tests
...                 Documentation URL to be defined

Resource            ../_resources/keywords/composition_keywords.robot
Resource            ../_resources/keywords/ehr_keywords.robot

Suite Teardown      restart SUT


*** Test Cases ***
Main flow create EHR
    Upload OPT ECIS    all_types/ehrn_family_history.opt
    Extract Template Id From OPT File
    Get Web Template By Template Id (ECIS)    ${template_id}
    Create ECIS EHR
    ${externalTemplate}    Set Variable    ${template_id}
    Set Test Variable    ${externalTemplate}
    [Teardown]    TRACE JIRA ISSUE    CDR-331

Main flow create and get EHR
    [Documentation]    Below keyword used to create EHR with OpenEHR endpoint.
    ...    As soon as CDR-331 is fixed, EHR must be created with EHRScape endpoint.
    [Tags]    not-ready
    #below keyword is used to create EHR with
    Create EHR From Valid Data Set    1    true    true    provided    not provided    not provided
    [Teardown]    TRACE JIRA ISSUE    CDR-331

Main flow create get and update EHR status
    [Documentation]    Create EHR, Get it and update EHR status
    ...    Blocked by CDR-331
    [Tags]    not-ready
    Fail    Create EHR using ecis endpoint is blocked by CDR-331
    [Teardown]    TRACE JIRA ISSUE    CDR-331


*** Keywords ***
##Below keywords are for EHR creation on OpenEHR endpoint:
create ehr from data table
    [Arguments]    ${subject}    ${is_modifiable}    ${is_queryable}    ${status_code}

    prepare new request session    Prefer=return=representation

    compose ehr_status    ${subject}    ${is_modifiable}    ${is_queryable}
    POST /ehr    ${ehr_status}
    check response    ${status_code}    ${is_modifiable}    ${is_queryable}

POST /ehr
    [Arguments]    ${body}=${None}
    &{response}    REST.POST    /ehr    ${body}
    Output Debug Info To Console

check response
    [Arguments]    ${status_code}    ${is_modifiable}    ${is_queryable}
    Integer    response status    ${status_code}

    # comment: changes is_modif./is_quer. to default expected values - boolean true
    IF    $is_modifiable=="${EMPTY}"
        ${is_modifiable}    Set Variable    ${TRUE}
    ELSE
        ${is_modifiable}    Set Variable    ${None}
    END
    IF    $is_queryable=="${EMPTY}"
        ${is_queryable}    Set Variable    ${TRUE}
    ELSE
        ${is_queryable}    Set Variable    ${None}
    END
    Boolean    response body ehr_status is_modifiable    ${is_modifiable}
    Boolean    response body ehr_status is_queryable    ${is_queryable}

compose ehr_status
    [Arguments]    ${subject}    ${is_modifiable}    ${is_queryable}

    set ehr_status subject    ${subject}
    set is_queryable / is_modifiable    ${is_modifiable}    ${is_queryable}
    Set Test Variable    ${ehr_status}    ${ehr_status}

Create EHR From Valid Data Set
    [Arguments]    ${No.}    ${queryable}    ${modifiable}    ${subject}    ${other_details}    ${ehrid}
    prepare new request session    Prefer=return=representation
    compose ehr payload    ${No.}    ${other_details}    ${modifiable}    ${queryable}
    create ehr    ${ehrid}
    validate response
    ...    ${No.}
    ...    ${queryable}
    ...    ${modifiable}
    ...    ${subject}
    ...    ${other_details}
    ...    ${ehrid}

compose ehr payload
    [Arguments]    ${No.}    ${other_details}    ${modifiable}    ${queryable}
    Log To Console    \n\nData Set No.: ${No.} \n\n

    # comment: use 000_ehr_status.json as blueprint for payload witho other_details
    IF    "${other_details}" == "not provided"
        ${payload}    randomize subject_id in test-data-set    valid/000_ehr_status.json

        # comment: use 000_ehr_status_with_other_details.json for payload with other_details
    ELSE IF    "${other_details}" == "provided"
        ${payload}    randomize subject_id in test-data-set    valid/000_ehr_status_with_other_details.json
    END

    ${payload=}    Update Value To Json    ${payload}    $.is_modifiable    ${modifiable}
    ${payload}    Update Value To Json    ${payload}    $.is_queryable    ${queryable}
    Output    ${payload}
    Set Test Variable    ${payload}    ${payload}

randomize subject_id in test-data-set
    [Arguments]    ${test_data_set}
    ${subject_id}    generate random id
    ${body}    Load JSON From File    ${EXECDIR}/robot/_resources/test_data_sets/ehr/${test_data_set}
    ${body}    Update Value To Json    ${body}    $..subject.external_ref.id.value    ${subject_id}
    RETURN    ${body}

generate random id
    # ${uuid}    Evaluate    str(uuid.uuid4())    uuid
    ${uuid}    Set Variable    ${{str(uuid.uuid4())}}
    RETURN    ${uuid}

validate response
    [Arguments]    ${No.}    ${queryable}    ${modifiable}    ${subject}    ${other_details}    ${ehrid}
    Log Many    ${No.}    ${queryable}    ${modifiable}    ${subject}    ${other_details}    ${ehrid}
    Integer    response status    201
    Object    response body system_id
    Object    response body ehr_id
    String    response body ehr_id value
    Object    response body time_created
    Object    response body ehr_status
    Object    response body ehr_status name
    String    response body ehr_status name value
    String    response body ehr_status archetype_node_id
    Object    response body ehr_status subject

    Boolean    response body ehr_status is_modifiable    ${modifiable}
    Boolean    response body ehr_status is_queryable    ${queryable}

    IF    "${other_details}" == "not provided"
        Missing    response body ehr_status other_details
    ELSE IF    "${other_details}" == "provided"
        Object    response body ehr_status other_details
    END
