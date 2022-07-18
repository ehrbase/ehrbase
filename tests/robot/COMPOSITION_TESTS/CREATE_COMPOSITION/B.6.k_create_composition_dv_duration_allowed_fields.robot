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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#422-test-case-dv_duration-xxx_allowed-field-constraints
...             ${\n}*4.2.2. Test case DV_DURATION xxx_allowed field constraints*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Variables ***
${composition_file}      minimal_instruction_1.composition.json
${opt_c_duration_patternPath}
...     .//attributes/children/attributes/children/attributes/children/attributes/children/item/pattern
${opt_reference_file}   minimal/minimal_instruction.opt
${positiveCode}     201
${negativeCode}     400

*** Test Cases ***
Test Allowed Fields Configured In C_DURATION
    [Tags]      not-ready   bug
    [Documentation]     *Documentation to be defined*
    [Template]      Test DV Duration Allowed Field Constraints
    PYMWDTHMS       P1Y                     ${positiveCode}
    PYMWDTHMS       P1Y3M                   ${positiveCode}
    PYMWDTHMS       P1Y3M15D                ${positiveCode}
    PYMWDTHMS       P1W                     ${positiveCode}
    PYMWDTHMS       P1Y3M15DT23H            ${positiveCode}
    PYMWDTHMS       P1Y3M15DT23H35M         ${positiveCode}
    PYMWDTHMS       P1Y3M15DT23H35M22S      ${positiveCode}
    PYMWDTHMS       P1W3D                   ${positiveCode}
    ##below are negative flows
    PMWDTHMS        P1Y                     ${negativeCode}
    PYWDTHMS        P1Y3M                   ${negativeCode}
    PYMWTHMS        P1Y3M15D                ${negativeCode}
    PYMDTHMS        P7W                     ${negativeCode}
    PYMWDTMS        P1Y3M15DT23H            ${negativeCode}
    PYMWDTHS        P1Y3M15DT23H35M         ${negativeCode}
    PYMWDTHM        P1Y3M15DT23H35M22S      ${negativeCode}
    PYMDTHMS        P1W3D                   ${negativeCode}
    #[Teardown]      PositiveCompositionTemplate     P1Y3M4D


*** Keywords ***
Precondition
    Upload OPT    minimal/minimal_instruction.opt
    create EHR

Test DV Duration Allowed Field Constraints
    [Arguments]     ${c_duration_opt_value}     ${dv_duration_composition_value}    ${expectedCode}
    [Documentation]     C_DURATION=${c_duration_opt_value}, DV_DURATION=${dv_duration_composition_value}, expectedCode=${expectedCode}
    Load XML File With OPT      ${opt_reference_file}
    ${returnedOptFile}   Change XML Value And Save Back To New OPT
    ...     ${xmlFileContent}   ${c_duration_opt_value}     ${opt_c_duration_patternPath}
    Upload OPT      ${returnedOptFile}
    create EHR
    CommitCompositionTemplate     ${dv_duration_composition_value}    ${expectedCode}
    [Teardown]      Remove File     ${newOPTFile}

CommitCompositionTemplate
    [Arguments]     ${dvDurationValue}      ${expectedCode}
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvDurationValue}
    commit composition      format=CANONICAL_JSON
    ...                     composition=${composition_file}
    Should Be Equal As Strings      ${response.status_code}     ${expectedCode}

Load XML File With OPT
    [Documentation]     Loads XML content from OPT file.
    ...     Stores file content in test variable, as well as full file path.
    [Arguments]     ${filePath}
    ${xmlFileContent}     Parse Xml   ${VALID DATA SETS}/${filePath}
    Set Test Variable   ${xmlFileContent}

Load Json File With Composition
    [Documentation]     Loads Json content from composition file.
    ...     Stores file content in test variable, as well as full file path.
    ${COMPO DATA SETS}     Set Variable
    ...     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}compositions
    ${file}                 Get File   ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    ${compositionFilePath}  Set Variable    ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    Set Test Variable       ${file}
    Set Test Variable       ${compositionFilePath}

Change Json KeyValue And Save Back To File
    [Documentation]     Updates $.content[0].activities[0].description.items[0].value.value to
    ...     value provided as argument.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on $.content[0].activities[0].description.items[0].value.value key
    [Arguments]     ${jsonContent}      ${valueToUpdate}
    ${objPath}      Set Variable        $.content[0].activities[0].description.items[0].value.value
    ${json_object}          Update Value To Json	${jsonContent}
    ...             ${objPath}        ${valueToUpdate}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}

Change XML Value And Save Back To New OPT
    [Documentation]     Updates XML text using xpath to
    ...     value provided as argument.
    ...     ${\n}Takes 3 arguments:
    ...     - XML file content
    ...     - text to be replaced with
    ...     - xPath expression.
    [Arguments]     ${xmlContent}   ${valueToUpdate}    ${xPathExpr}
    Set Test Variable   ${newOPTFile}   ${VALID DATA SETS}/minimal/newly_generated_file.opt
    ${patternElement}   Get Element	    ${xmlContent}	xpath=${xPathExpr}
    Log     Initial C_DURATION pattern value is = ${patternElement.text}
    Set Element Text   ${xmlContent}   text=${valueToUpdate}     xpath=${xPathExpr}
    ${patternElementChanged}       Get Element	    ${xmlContent}	xpath=${xPathExpr}
    Log     Modified C_DURATION pattern value is = ${patternElementChanged.text}
    Should Be Equal    ${patternElementChanged.text}       ${valueToUpdate}
    Save Xml    ${xmlContent}   ${newOPTFile}
    [return]    minimal/newly_generated_file.opt

