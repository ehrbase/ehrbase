# Copyright (c) 2022 Vladislav Ploaia
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
Resource            ../_resources/keywords/aql_query_keywords.robot

Suite Teardown      restart SUT


*** Test Cases ***
Main flow create and update Composition
    [Documentation]     Create and Update Composition using EHRScape endpoints.
    Create Template     all_types/ehrn_family_history.opt
    Extract Template Id From OPT File
    Get Web Template By Template Id (ECIS)      ${template_id}
    create EHR
    Set Test Variable   ${externalTemplate}     ${template_id}
    ## Create action
    commit composition  format=FLAT
    ...    composition=ehrn_family_history__.json
    ...    extTemplateId=true
    check the successful result of commit composition
    Set Test Variable   ${response}    ${response.json()}
    ${compoUid}     Set Variable     ${response["compositionUid"]}
    ${compoUidWithURL}  Fetch From Left     ${compoUid}         ::1
    ${compoUidURL}      Fetch From Right    ${compoUidWithURL}  ::
    Should Contain      ${compoUid}      ${compoUidURL}
    ## Get composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    Set Test Variable   ${response}    ${response.json()}
    Should Be Equal As Strings    ${compoUid}   ${response["compositionUid"]}
    ## Update composition
    Update Composition (FLAT)    new_version_of_composition=ehrn_family_history.v2__.json
    ## Get composition after update
    (FLAT) get composition by composition_uid    ${composition_uid}
    check composition exists
    Set Test Variable   ${response}    ${response.json()}
    Should Contain      ${response["compositionUid"]}   ${compoUidURL}
    ## Check query endpoint for COMPOSITION
    #${query}=           Catenate
    #...                 SELECT
    #...                 c as COMPOSITION
    #...                 FROM EHR e
    #...                 CONTAINS composition c
    #Set Test Variable    ${payload}    {"aql": "${query}"}
    #POST /query (REST) - ECIS    JSON
    #Integer    response status    200
    #Log     ${response.json()}
    #Should Be Equal As Strings     ${response.json()['action']}   RETRIEVE
    #Should Contain      ${response.json()['compositionUid']}      ${compoUidURL}

Main flow create and delete Composition
    [Documentation]     Create and Update Composition using EHRScape endpoints.
    Create Template    all_types/family_history.opt
    Extract Template Id From OPT File
    Get Web Template By Template Id (ECIS)      ${template_id}
    create EHR
    Set Test Variable   ${externalTemplate}     ${template_id}
    commit composition    format=FLAT
    ...    composition=family_history__.json
    ...    extTemplateId=true
    check the successful result of commit composition
    Set Test Variable   ${response}     ${response.json()}
    ${compoUid}     Set Variable        ${response["compositionUid"]}
    ${compoUidWithURL}  Fetch From Left     ${compoUid}         ::1
    ${compoUidURL}      Fetch From Right    ${compoUidWithURL}  ::
    Should Contain      ${compoUid}         ${compoUidURL}
    (FLAT) get composition by composition_uid       ${composition_uid}
    ## Delete action
    delete composition  ${composition_uid}      ehrScape=true
    get deleted composition (EHRScape)

Create Composition With Period Having Fractional Unit
    [Documentation]     Create Composition with Fractional Unit, using EHRScape endpoints.
    [Tags]      not-ready   bug
    Create Template     all_types/medications_statement.v0.opt
    Extract Template Id From OPT File
    Get Web Template By Template Id (ECIS)      ${template_id}
    create EHR
    Set Test Variable   ${externalTemplate}     ${template_id}
    ${composition_file}         Set Variable    medications_statement.v0__.json
    ${composition_file_tmp}     Set Variable    medications_statement.v0.tmp__.json
    ${compo_file_path}          Set Variable    ${COMPO DATA SETS}/FLAT

    ${expected}     Load JSON From File    ${compo_file_path}/${composition_file}
                    Log     ${expected['medications/medication_list/medication_statement:0/timing_-_non-daily/repetition_interval']}
                    Update Value To Json   ${expected}
                    ...     ['medications/medication_list/medication_statement:0/timing_-_non-daily/repetition_interval']
                    ...     P1.5Y
                    #...     P1.5Y
    ${json_str}     Convert JSON To String    ${expected}
    Create File     ${compo_file_path}/${composition_file_tmp}    ${json_str}
    commit composition    format=FLAT
    ...    composition=${composition_file_tmp}
    ...    extTemplateId=true
    Remove File     ${compo_file_path}/${composition_file_tmp}
    check the successful result of commit composition
    (FLAT) get composition by composition_uid       ${composition_uid}
    Should Be Equal As Strings
    ...     ${response.json()['composition']['medications/medication_list/medication_statement:0/timing_-_non-daily/repetition_interval']}
    ...     P1.5Y
    [Teardown]      TRACE GITHUB ISSUE    879


*** Keywords ***
Create Template
    [Arguments]    ${fileLocation}
    Upload OPT ECIS    ${fileLocation}
