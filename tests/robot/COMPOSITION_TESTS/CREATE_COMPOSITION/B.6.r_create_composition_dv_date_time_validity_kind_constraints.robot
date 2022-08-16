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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#452-test-case-dv_date_time-validity-kind-constraint
...             ${\n}*4.5.2. Test Case DV_DATE_TIME validity kind constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Variables ***
${composition_file}     Test_dv_date_time_validity_kind_constraint_v0__.json
${opt_c_date_time_patternPath}
...     .//children[1]/item[1]/pattern[1]
${opt_reference_file}   all_types/Test_dv_date_time_validity_kind_constraint_v0.opt
${opt_temp_file}        all_types/Test_dv_date_time_validity_kind_constraint_v0__temp.opt
${positiveCode}     201
${negativeCode}     400


*** Test Cases ***
Test DV Date Time Validity Kind Constraints - C_DATE_TIME With Configured Pattern
    [Tags]      not-ready   bug
    [Documentation]     *Documentation to be defined*
    [Template]      Configure And Commit DV Date Time Validity Kind Constraints - C_DATE_TIME With Configured Pattern
    #C_DATE_TIME pattern value, DV_DATE_TIME value in COMPOSITION, expected code
    yyyy-mm-ddTHH:MM:SS     2021    ${negativeCode}
    yyyy-mm-ddTHH:MM:??     2021    ${negativeCode}
    yyyy-mm-ddT??:??:??     2021    ${negativeCode}
    yyyy-mm-??T??:??:??     2021    ${negativeCode}
    yyyy-mm-ddTHH:MM:XX     2021    ${negativeCode}
    yyyy-mm-XXTXX:XX:XX     2021    ${negativeCode}
    yyyy-??-??T??:??:??     2021    ${positiveCode}
    yyyy-XX-XXTXX:XX:XX     2021    ${positiveCode}

    yyyy-mm-ddTHH:MM:SS     2021-10     ${negativeCode}
    yyyy-mm-ddTHH:??:??     2021-10     ${negativeCode}
    yyyy-mm-ddTHH:MM:XX     2021-10     ${negativeCode}
    yyyy-mm-ddTXX:XX:XX     2021-10     ${negativeCode}
    yyyy-XX-XXTXX:XX:XX     2021-10     ${negativeCode}
    yyyy-mm-XXTXX:XX:XX     2021-10     ${positiveCode}
    yyyy-??-??T??:??:??     2021-10     ${positiveCode}

    yyyy-mm-ddTHH:??:??     2021-10-24      ${negativeCode}
    yyyy-mm-ddTHH:MM:XX     2021-10-24      ${negativeCode}
    yyyy-XX-XXTXX:XX:XX     2021-10-24      ${negativeCode}
    yyyy-mm-ddT??:??:??     2021-10-24      ${positiveCode}
    yyyy-??-??T??:??:??     2021-10-24      ${positiveCode}
    yyyy-mm-ddTXX:XX:XX     2021-10-24      ${positiveCode}

    yyyy-mm-ddTHH:MM:SS     2021-10-24T10       ${negativeCode}
    yyyy-mm-ddTHH:MM:??     2021-10-24T10       ${negativeCode}
    yyyy-mm-ddTHH:MM:XX     2021-10-24T10       ${negativeCode}
    yyyy-XX-XXTXX:XX:XX     2021-10-24T10       ${negativeCode}
    yyyy-mm-ddTHH:??:??     2021-10-24T10       ${positiveCode}
    yyyy-mm-??T??:??:??     2021-10-24T10       ${positiveCode}
    yyyy-mm-ddTHH:XX:XX     2021-10-24T10       ${positiveCode}

    yyyy-mm-ddTHH:MM:SS     2021-10-24T10:30        ${negativeCode}
    yyyy-mm-ddTHH:XX:XX     2021-10-24T10:30        ${negativeCode}
    yyyy-mm-XXTXX:XX:XX     2021-10-24T10:30        ${negativeCode}
    yyyy-mm-ddTHH:MM:??     2021-10-24T10:30        ${positiveCode}
    yyyy-mm-ddT??:??:??     2021-10-24T10:30        ${positiveCode}
    yyyy-??-??T??:??:??     2021-10-24T10:30        ${positiveCode}
    yyyy-mm-ddTHH:MM:XX     2021-10-24T10:30        ${positiveCode}

*** Keywords ***
Configure And Commit DV Date Time Validity Kind Constraints - C_DATE_TIME With Configured Pattern
    [Arguments]     ${c_date_time_pattern_opt_value}     ${dv_date_time_composition_value}
    ...             ${expectedCode}
    [Documentation]     C_DATE_TIME_PATTERN=${c_date_time_pattern_opt_value},
    ...     DV_DATE_TIME_VALUE=${dv_date_time_composition_value},
    ...     expectedCode=${expectedCode}
    Load XML File With OPT      ${opt_reference_file}
    ${returnedOptFile}   Change XML Value And Save Back To New OPT
    ...     ${xmlFileContent}
    ...     ${c_date_time_pattern_opt_value}
    ...     ${opt_c_date_time_patternPath}
    Upload OPT      ${returnedOptFile}
    create EHR
    ${statusCodeBoolean}    CommitCompositionTemplate     ${dv_date_time_composition_value}    ${expectedCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedCode} is different.
    END
    [Teardown]      Run Keywords
    ...     Delete Composition Using API    AND
    ...     Delete Template Using API   AND
    ...     Remove File     ${newOPTFile}

Load XML File With OPT
    [Documentation]     Loads XML content from OPT file.
    ...     Stores file content in test variable, as well as full file path.
    [Arguments]     ${filePath}
    ${xmlFileContent}     Parse Xml   ${VALID DATA SETS}/${filePath}
    Set Test Variable   ${xmlFileContent}

CommitCompositionTemplate
    [Arguments]     ${dvDateTimeValue}      ${expectedCode}
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvDateTimeValue}
    commit composition      format=CANONICAL_JSON
    ...                     composition=${composition_file}
    ${isStatusCodeEqual}    Run Keyword And Return Status
    ...     Should Be Equal As Strings      ${response.status_code}     ${expectedCode}
    ${isUidPresent}     Run Keyword And Return Status   Set Test Variable   ${version_uid}    ${response.json()['uid']['value']}
    IF      ${isUidPresent} == ${TRUE}
        ${short_uid}        Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                            Set Suite Variable   ${versioned_object_uid}    ${short_uid}
    ELSE
        Set Suite Variable   ${versioned_object_uid}    ${None}
    END
    [Return]    ${isStatusCodeEqual}

Change Json KeyValue And Save Back To File
    [Documentation]     Updates DV_DATE_TIME values to
    ...     value provided as argument.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be changed JsonPath key
    [Arguments]     ${jsonContent}      ${valueToUpdate}
    ${objPath}      Set Variable        $.content[0].data.events[0].time.value
    ${json_object}  Update Value To Json	${jsonContent}
    ...             ${objPath}        ${valueToUpdate}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}

Change XML Value And Save Back To New OPT
    [Documentation]     Updates XML text using xpath to
    ...     value provided as argument.
    ...     ${\n}Takes 3 arguments:
    ...     - XML file content
    ...     - c_date_time value
    ...     - c_date_time value xPath
    [Arguments]     ${xmlContent}   ${c_date_time_pattern_value}
    ...     ${c_date_time_pattern_path}
    Set Test Variable   ${newOPTFile}   ${VALID DATA SETS}/${opt_temp_file}
    ${patternElement}       Get Element	    ${xmlContent}	xpath=${c_date_time_pattern_path}
    Log     Initial C_DATE_TIME pattern value is = ${patternElement.text}
    Set Element Text   ${xmlContent}   text=${c_date_time_pattern_value}     xpath=${c_date_time_pattern_path}
    ${patternElementChanged}       Get Element	    ${xmlContent}	xpath=${c_date_time_pattern_path}
    Log     Modified C_DATE_TIME pattern value is = ${patternElementChanged.text}
    Should Be Equal As Strings    ${patternElementChanged.text}       ${c_date_time_pattern_value}
    Save Xml    ${xmlContent}   ${newOPTFile}
    [return]    ${opt_temp_file}

Load Json File With Composition
    [Documentation]     Loads Json content from composition file.
    ...     Stores file content in test variable, as well as full file path.
    ${COMPO DATA SETS}     Set Variable
    ...     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}compositions
    ${file}                 Get File        ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    ${compositionFilePath}  Set Variable    ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    Set Suite Variable       ${file}
    Set Suite Variable       ${compositionFilePath}

Delete Template Using API
    &{resp}=            REST.DELETE   ${admin_baseurl}/template/${template_id}
                        Set Suite Variable    ${deleteTemplateResponse}    ${resp}
                        Output Debug Info To Console
                        Should Be Equal As Strings      ${resp.status}      200
                        Delete All Sessions

Delete Composition Using API
    IF      '${versioned_object_uid}' != '${None}'
        &{resp}         REST.DELETE    ${admin_baseurl}/ehr/${ehr_id}/composition/${versioned_object_uid}
                        Run Keyword And Return Status   Integer    response status    204
                        Set Suite Variable    ${deleteCompositionResponse}    ${resp}
                        Output Debug Info To Console
    END