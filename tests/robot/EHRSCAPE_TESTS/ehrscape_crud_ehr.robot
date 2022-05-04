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

Suite Setup         restart SUT
Suite Teardown      restart SUT


*** Test Cases ***
Main Flow Create EHR
    [Documentation]     Create EHR using EHRScape endpoint.
    [Tags]    PostEhr    EHRSCAPE
    Upload OPT ECIS    all_types/ehrn_family_history.opt
    Extract Template Id From OPT File
    Get Web Template By Template Id (ECIS)    ${template_id}
    Create ECIS EHR
    Log     ${response.json()}
    ${ehrId}    Collections.Get From Dictionary    ${response.json()}    ehrId
    Set Suite Variable      ${ehr_id}       ${ehrId}
    ${externalTemplate}    Set Variable    ${template_id}
    Set Test Variable    ${externalTemplate}

Get EHR Using Ehr Id And By Subject Id, Namespace
    [Documentation]    1. Get existing EHR using Ehr Id.
    ...     2. Get existing EHR using Subject Id and Subject Namespace criteria.
    ...     *Dependency*: Test Case -> Main Flow Create EHR, ehr_id suite variable.
    [Tags]    GetEhr    EHRSCAPE
    Retrieve EHR By Ehr Id (ECIS)
    Retrieve EHR By Subject Id And Subject Namespace (ECIS)
    ...     subject_id=74777-1259      subject_namespace=testIssuer

Get EHR And Update EHR Status
    [Documentation]    Create EHR, Get it and update EHR status.
    [Tags]    UpdateEhrStatus   EHRSCAPE
    ${ehr_status_json}      Load JSON From File
                            ...     ${VALID EHR DATA SETS}/000_ehr_status_ecis.json
    Update EHR Status (ECIS)    ${ehr_id}   ${ehr_status_json}
    Should Be Equal As Strings  ${response["body"]["action"]}    UPDATE
    Should Be Equal             ${response["body"]["ehrStatus"]["modifiable"]}          ${True}
    Should Be Equal             ${response["body"]["ehrStatus"]["queryable"]}           ${True}
    Should Be Equal As Strings  ${response["body"]["ehrStatus"]["subjectId"]}           74777-1258
    Should Be Equal As Strings  ${response["body"]["ehrStatus"]["subjectNamespace"]}    testIssuerModified
