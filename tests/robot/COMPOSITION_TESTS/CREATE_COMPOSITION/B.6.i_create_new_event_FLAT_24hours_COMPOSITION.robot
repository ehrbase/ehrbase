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
...             Tests for bug: https://jira.vitagroup.ag/browse/CDR-312
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot

Suite Setup         Precondition
Suite Teardown      restart SUT

*** Variables ***
${composition_file}      a1__24hour_average_value.json

*** Test Cases ***
Create new event COMPOSITION FLAT with 24 Hours Average - 24 value
    [Tags]
    ${24HoursAvg_Value}     Set Variable    PT24H
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    Change Json 24 Hour Average Value and Save Back To File
    ...     ${initalJson}      ${24HoursAvg_Value}
    commit composition      format=FLAT
    ...                     composition=${composition_file}
    Log     ${response.content}
    Status Should Be        201
    check the successful result of commit composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    check content of updated composition generic (JSON)
    ...     ['composition']['a1/blood_pressure/a24_hour_average/width']
    ...     ${24HoursAvg_Value}

## below test cases are testing negative flows, with value < 24H and > 24H
Create new event COMPOSITION FLAT with 24 Hours Average - 15 value
    [Tags]      negative
    ${24HoursAvg_Value}     Set Variable    PT15H
    Create Composition With 24 Hours Average - Invalid Value    ${24HoursAvg_Value}
    Should Contain    ${errMsg}    The value ${24HoursAvg_Value} must be >= PT24H


Create new event COMPOSITION FLAT with 24 Hours Average - 0 value
    [Tags]      negative
    ${24HoursAvg_Value}     Set Variable    PT0S
    Create Composition With 24 Hours Average - Invalid Value    ${24HoursAvg_Value}
    Should Contain    ${errMsg}    The value ${24HoursAvg_Value} must be >= PT24H


Create new event COMPOSITION FLAT with 24 Hours Average - 27 value
    [Tags]      negative
    ${24HoursAvg_Value}     Set Variable    PT27H
    Create Composition With 24 Hours Average - Invalid Value    ${24HoursAvg_Value}
    Should Contain    ${errMsg}    The value ${24HoursAvg_Value} must be <= PT24H


Create new event COMPOSITION FLAT with 24 Hours Average - -1 value
    [Tags]      negative
    ${24HoursAvg_Value}     Set Variable    PT-1H
    Create Composition With 24 Hours Average - Invalid Value    ${24HoursAvg_Value}
    Should Contain    ${errMsg}    The value ${24HoursAvg_Value} must be >= PT24H


Create new event COMPOSITION FLAT with 24 Hours Average - -24 value
    [Tags]      negative
    ${24HoursAvg_Value}     Set Variable    PT-24H
    Create Composition With 24 Hours Average - Invalid Value    ${24HoursAvg_Value}
    Should Contain    ${errMsg}    The value ${24HoursAvg_Value} must be >= PT24H


*** Keywords ***
Precondition
    upload OPT    all_types/opt_24h_average.opt
    create EHR

Create Composition With 24 Hours Average - Invalid Value
    [Arguments]     ${24HoursVal}
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    Change Json 24 Hour Average Value and Save Back To File
    ...     ${initalJson}      ${24HoursVal}
    commit composition      format=FLAT
    ...                     composition=${composition_file}
    Log     ${response.content}
    Status Should Be        400
    ${errType}      Set Variable     ${response.json()['error']}
    Should Be Equal As Strings     ${errType}      Bad Request
    ${errMsg}      Set Variable     ${response.json()['message']}
    Set Test Variable       ${errType}
    Set Test Variable       ${errMsg}

Load Json File With Composition
    [Documentation]     Loads Json content from composition file.
    ...     Stores file content in test variable, as well as full file path.
    ${COMPO DATA SETS}     Set Variable
    ...     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}compositions
    ${file}                 Get File   ${COMPO DATA SETS}/FLAT/${composition_file}
    ${compositionFilePath}  Set Variable    ${COMPO DATA SETS}/FLAT/${composition_file}
    Set Test Variable       ${file}
    Set Test Variable       ${compositionFilePath}

Change Json 24 Hour Average Value and Save Back To File
    [Documentation]     Updates a1/blood_pressure/a24_hour_average/width value to
    ...     value provided as argument.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be provided for a1/blood_pressure/a24_hour_average/width key
    [Arguments]     ${jsonContent}      ${valueToUpdate}
    ${objPath}      Set Variable        ['a1/blood_pressure/a24_hour_average/width']
    ${json_object}          Update Value To Json	${jsonContent}
    ...             ${objPath}        ${valueToUpdate}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
