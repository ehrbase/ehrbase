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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#424-test-case-dv_duration-fields-allowed-and-range-constraints-combined
...             ${\n}*4.2.4. Test case DV_DURATION fields allowed and range constraints combined*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Variables ***
${composition_file}      minimal_instruction_2.composition.json
${opt_c_duration_patternPath}
...     .//attributes/children/attributes/children/attributes/children/attributes/children/item/pattern
${range_lower_patternPath}
...     .//attributes/children/attributes/children/attributes/children/attributes/children/item/range/lower
${range_upper_patternPath}
...     .//attributes/children/attributes/children/attributes/children/attributes/children/item/range/upper
${opt_reference_file}   minimal/minimal_instruction.dv_duration_fields_allowed.range_constraints.opt
${positiveCode}     201
${negativeCode}     400


*** Test Cases ***
Test Allowed Fields And Range Constraints Configured In C_DURATION
    [Tags]      not-ready   bug
    [Documentation]     *Documentation to be defined*
    [Template]      Test DV Duration Allowed Fields And Range Constraints
    #C_DURATION pattern value, DV_DURATION value in COMPOSITION, C_DURATION range lower, C_DURATION range upper, expected code
    PYMWDTHMS       P1Y         P0Y     P50Y    ${positiveCode}
    PYMWDTHMS       P1Y3M       P2Y     P50Y    ${positiveCode}
    ##below are negative flows
    PYMWDTHMS       P1Y         P2Y     P50Y    ${negativeCode}
    PMWDTHMS        P1Y         P0Y     P50Y    ${negativeCode}
    PMWDTHMS        P1Y         P2Y     P50Y    ${negativeCode}
    PYWDTHMS        P1Y3M       P1Y     P50Y    ${negativeCode}
    PYMWDTHMS       P1Y3M       P3Y     P50Y    ${negativeCode}
    PYWDTHMS        P1Y3M       P3Y     P50Y    ${negativeCode}
    PYMWDTHMS       PT2M43.5S   PT1M    PT60M   ${negativeCode}


*** Keywords ***
Test DV Duration Allowed Fields And Range Constraints
    [Arguments]     ${c_duration_opt_value}     ${dv_duration_composition_value}
    ...             ${range_lower}      ${range_upper}    ${expectedCode}
    [Documentation]     C_DURATION=${c_duration_opt_value}, DV_DURATION=${dv_duration_composition_value}
    ...     ${\n}rangeLower=${range_lower}, rangeUpper=${range_upper}, expectedCode=${expectedCode}
    Load XML File With OPT      ${opt_reference_file}
    ${returnedOptFile}   Change XML Value And Save Back To New OPT
    ...     ${xmlFileContent}
    ...     ${c_duration_opt_value}     ${opt_c_duration_patternPath}
    ...     ${range_lower}      ${range_lower_patternPath}
    ...     ${range_upper}      ${range_upper_patternPath}
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
    ...     ${\n}Takes 7 arguments:
    ...     - XML file content
    ...     - c_duration value
    ...     - c_duration value xPath
    ...     - c_duration range lower value
    ...     - c_duration range lower xPath
    ...     - c_duration range upper value
    ...     - c_duration range upper xPath
    [Arguments]     ${xmlContent}   ${c_duration_pattern_value}    ${c_duration_pattern_path}
    ...     ${range_lower_value}    ${range_lower_path}
    ...     ${range_upper_value}    ${range_upper_path}
    Set Test Variable   ${newOPTFile}   ${VALID DATA SETS}/minimal/newly_generated_file_range_combinations.opt
    ${patternElement}       Get Element	    ${xmlContent}	xpath=${c_duration_pattern_path}
    ${rangeLowerElement}    Get Element	    ${xmlContent}	xpath=${range_lower_path}
    ${rangeUpperElement}    Get Element	    ${xmlContent}	xpath=${range_upper_path}
    Log     C_DURATION pattern value is = ${patternElement.text}    console=yes
    Log     range lower value is = ${rangeLowerElement.text}        console=yes
    Log     range upper value is = ${rangeUpperElement.text}        console=yes
    Set Element Text   ${xmlContent}   text=${c_duration_pattern_value}     xpath=${c_duration_pattern_path}
    Set Element Text   ${xmlContent}   text=${range_lower_value}            xpath=${range_lower_path}
    Set Element Text   ${xmlContent}   text=${range_upper_value}            xpath=${range_upper_path}
    Save Xml    ${xmlContent}   ${newOPTFile}
    [return]    minimal/newly_generated_file_range_combinations.opt