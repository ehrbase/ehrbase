# Copyright (c) 2022 Vladislav Ploaia (Vitagroup - CDR Core Team)
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
...             ${\n}Based on:
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#421-test-case-dv_duration-open-constraint
...             ${\n}*Covers 4.2.1. Test case DV_DURATION open constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot

Suite Setup       Precondition


*** Variables ***
${composition_file}      Test_all_types_v2__.json


*** Test Cases ***
Create Composition With DV_DURATION Combinations - Positive
    [Documentation]     *Operations done here (Positive flows):*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_DURATION using ${dvDurationValue} argument value
    ...     - commit composition
    ...     - check status code of the commited composition to be 201.
    ...     *Postcondition:* Add DV_DURATION value P1Y3M4D, to ${composition_file}.
    [Tags]      not-ready   bug
    [Template]      PositiveCompositionTemplate
    P1Y
    P1Y3M
    P1W
    P1Y3M4D
    P1Y3M4DT2H
    P1Y3M4DT2H14M
    P1Y3M4DT2H14M5S
    P1Y3M4DT2H14M15.5S
    P3M1W
    -P2M    #fails
    # Jira bug: https://jira.vitagroup.ag/browse/CDR-466
    [Teardown]      PositiveCompositionTemplate     P1Y3M4D

Create Composition With DV_DURATION Combinations - Negative
    [Documentation]     *Operations done here (Negative flows):*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_DURATION using ${dvDurationValue} argument value
    ...     - commit composition
    ...     - check status code of the commited composition to be 400.
    ...     *Postcondition:* Add DV_DURATION value P1Y3M4D, to ${composition_file}.
    [Template]      NegativeCompositionTemplate
    ${EMPTY}
    1Y
    P1Y3M4DT2H14.5M
    P1Y3M4DT2.5H
    [Teardown]      PositiveCompositionTemplate     P1Y3M4D


*** Keywords ***
Precondition
    Upload OPT    all_types/Test_all_types_v2.opt
    create EHR

PositiveCompositionTemplate
    [Arguments]     ${dvDurationValue}=P1Y3M4D
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvDurationValue}
    commit composition      format=CANONICAL_JSON
    ...                     composition=${composition_file}
    Should Be Equal As Strings      ${response.status_code}     201

NegativeCompositionTemplate
    [Arguments]     ${dvDurationValue}=1Y
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvDurationValue}
    commit composition      format=CANONICAL_JSON
    ...                     composition=${composition_file}
    Should Be Equal As Strings      ${response.status_code}     400

Load Json File With Composition
    [Documentation]     Loads Json content from composition file.
    ...     Stores file content in test variable, as well as full file path.
    ${COMPO DATA SETS}     Set Variable
    ...     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}compositions
    ${file}                 Get File   ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    ${compositionFilePath}  Set Variable    ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    Set Test Variable       ${file}
    Set Test Variable       ${compositionFilePath}

Change Json KeyValue and Save Back To File
    [Documentation]     Updates $.content[0].data.events[0].data.items[11].value.value to
    ...     value provided as argument.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on $.content[0].data.events[0].data.items[11].value.value key
    [Arguments]     ${jsonContent}      ${valueToUpdate}
    ${objPath}      Set Variable        $.content[0].data.events[0].data.items[11].value.value
    ${json_object}          Update Value To Json	${jsonContent}
    ...             ${objPath}        ${valueToUpdate}
    ${changedDvDurationValue}   Get Value From Json     ${jsonContent}      ${objPath}
    Should Be Equal As Strings     ${changedDvDurationValue[0]}   ${valueToUpdate}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}