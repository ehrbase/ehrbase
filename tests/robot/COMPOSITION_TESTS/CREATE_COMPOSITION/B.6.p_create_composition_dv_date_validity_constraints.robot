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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#442-test-case-dv_date-validity-kind-constraint
...             ${\n}*4.4.2. Test Case DV_DATE validity kind constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Variables ***
${composition_file}      Test_all_types_v2__.json
${opt_c_date_patternPath}
...     .//attributes//attributes//attributes//attributes//attributes/children/attributes[2]/children/attributes/children[1]//item
${opt_reference_file}
...     all_types/Test_all_types_v2.opt
${opt_temp_file}
...     all_types/Test_all_types_v2__temp.opt
${positiveCode}     201
${negativeCode}     400


*** Test Cases ***
Test DV_DATE With Constraints On Month And/Or Day Configured In C_DATE
    [Tags]      not-ready   bug
    [Documentation]     *Documentation to be defined*
    [Template]      Test DV_DATE With Constraints On Month And/Or Day
    yyyy-mm-dd      2022            ${negativeCode}
    yyyy-mm-??      2022            ${negativeCode}
    yyyy-mm-XX      2022            ${negativeCode}
    yyyy-mm-dd      2022-10         ${negativeCode}
    yyyy-XX-XX      2022-10         ${negativeCode}
    yyyy-mm-XX      2022-10-24      ${negativeCode}
    yyyy-XX-XX      2022-10-24      ${negativeCode}

    yyyy-??-??      2022            ${positiveCode}
    yyyy-XX-XX      2022            ${positiveCode}
    yyyy-mm-??      2022-10         ${positiveCode}
    yyyy-??-??      2022-10         ${positiveCode}
    yyyy-mm-XX      2022-10         ${positiveCode}
    yyyy-mm-dd      2022-10-24      ${positiveCode}
    yyyy-mm-??      2022-10-24      ${positiveCode}
    yyyy-??-??      2022-10-24      ${positiveCode}
    [Teardown]      TRACE JIRA ISSUE    CDR-498

*** Keywords ***
Test DV_DATE With Constraints On Month And/Or Day
    [Arguments]     ${c_date_opt_value}     ${dv_date_composition_value}    ${expectedCode}
    [Documentation]     C_DATE=${c_date_opt_value}, DV_DATE=${dv_date_composition_value}
    ...     ${\n}expectedCode=${expectedCode}
    Load XML File With OPT      ${opt_reference_file}
    ${returnedOptFile}   Change XML Value And Save Back To New OPT
    ...     ${xmlFileContent}
    ...     ${c_date_opt_value}     ${opt_c_date_patternPath}
    Upload OPT      ${returnedOptFile}
    create EHR
    CommitCompositionTemplate     ${dv_date_composition_value}    ${expectedCode}
    [Teardown]      Remove File     ${newOPTFile}

CommitCompositionTemplate
    [Arguments]     ${dvDateValue}      ${expectedCode}
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvDateValue}
    commit composition      format=CANONICAL_JSON
    ...                     composition=${composition_file}
    Should Be Equal As Strings      ${response.status_code}     ${expectedCode}

Load XML File With OPT
    [Documentation]     Loads XML content from OPT file.
    ...     Stores file content in test variable, as well as full file path.
    [Arguments]     ${filePath}
    ${xmlFileContent}     Parse Xml   ${VALID DATA SETS}/${filePath}
    Set Test Variable   ${xmlFileContent}

Change Json KeyValue And Save Back To File
    [Documentation]     Updates $.content[0].data.events[0].data.items[5].value.value to
    ...     value provided as argument.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on $.content[0].data.events[0].data.items[5].value.value key
    [Arguments]     ${jsonContent}      ${valueToUpdate}
    ${objPath}      Set Variable        $.content[0].data.events[0].data.items[5].value.value
    ${json_object}          Update Value To Json	${jsonContent}
    ...             ${objPath}        ${valueToUpdate}
    ${json_object}          Update Value To Json	${jsonContent}
    ...             $.content[2].items[0].items[0].items[0].activities[0].description.items[0].value.value
    ...             ${valueToUpdate}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}

Change XML Value And Save Back To New OPT
    [Documentation]     Updates XML text using xpath to
    ...     value provided as argument.
    ...     ${\n}Takes 3 arguments:
    ...     - XML file content
    ...     - c_date value
    ...     - c_date value xPath.
    [Arguments]     ${xmlContent}   ${c_date_pattern_value}    ${c_date_pattern_path}
    Set Test Variable   ${newOPTFile}   ${VALID DATA SETS}/${opt_temp_file}
    ${patternElement}       Get Element	    ${xmlContent}	xpath=${c_date_pattern_path}
    Log     Initial C_DATE pattern value is = ${patternElement.text}
    Set Element Text   ${xmlContent}   text=${c_date_pattern_value}     xpath=${c_date_pattern_path}
    ${patternElementChanged}       Get Element	    ${xmlContent}	xpath=${c_date_pattern_path}
    Log     Modified C_DATE pattern value is = ${patternElementChanged.text}
    Should Be Equal As Strings    ${patternElementChanged.text}       ${c_date_pattern_value}
    Save Xml    ${xmlContent}   ${newOPTFile}
    [return]    ${opt_temp_file}