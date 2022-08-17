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
    yyyy-mm-ddTHH:MM:XX     2021    ${negativeCode}
    yyyy-mm-XXTXX:XX:XX     2021    ${negativeCode}
    yyyy-??-??T??:??:??     2021    ${positiveCode}
    yyyy-XX-XXTXX:XX:XX     2021    ${positiveCode}

    yyyy-mm-ddTHH:MM:SS     2021-10     ${negativeCode}
    yyyy-mm-ddTHH:??:??     2021-10     ${negativeCode}
    yyyy-mm-ddTHH:MM:XX     2021-10     ${negativeCode}
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
    [Teardown]      TRACE JIRA ISSUE    CDR-513

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
    Commit Composition Using Robot Templates
    ...                     format=CANONICAL_JSON
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
    ${objPath2}     Set Variable        $.content[0].data.events[0].data.items[0].value.value
    ${json_object}  Update Value To Json	${jsonContent}
    ...             ${objPath}        ${valueToUpdate}
    ${json_object}  Update Value To Json	${jsonContent}
    ...             ${objPath2}       ${valueToUpdate}
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

Commit Composition Using Robot Templates
    [Arguments]         ${format}   ${composition}
    ...         ${need_template_id}=true   ${prefer}=representation
    ...         ${lifecycle}=complete    ${extTemplateId}=false
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 DEPENDENCY: `upload OPT`, `create EHR`
    ...
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition
    ...
    ...                 FORMAT VARIABLES: FLAT, TDD, STRUCTURED, CANONICAL_JSON, CANONICAL_XML

    @{template}=        Split String    ${composition}   __
    ${template}=        Get From List   ${template}      0

    Set Suite Variable    ${template_id}    ${template}

    ${file}=           Get File   ${COMPO DATA SETS}/${format}/${composition}

    &{headers}=        Create Dictionary   Prefer=return=${prefer}
    ...                openEHR-VERSION.lifecycle_state=${lifecycle}

    IF    '${need_template_id}' == 'true'
        Set To Dictionary   ${headers}   openEHR-TEMPLATE_ID=${template}
    END

    IF   '${format}'=='CANONICAL_JSON'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/json
        Set To Dictionary   ${headers}   Accept=application/json
    ELSE IF   '${format}'=='CANONICAL_XML'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/xml
        Set To Dictionary   ${headers}   Accept=application/xml
    ELSE IF   '${format}'=='FLAT'
        Set To Dictionary   ${headers}   Content-Type=application/json
        Set To Dictionary   ${headers}   Accept=application/json
        Set To Dictionary   ${headers}   X-Forwarded-Host=example.com
        Set To Dictionary   ${headers}   X-Forwarded-Port=333
        Set To Dictionary   ${headers}   X-Forwarded-Proto=https
        IF  '${extTemplateId}' == 'true'
            ${template_id}      Set Variable   ${externalTemplate}
            ${template}      Set Variable   ${externalTemplate}
            Set Suite Variable    ${template_id}    ${template}
        END
        &{params}       Create Dictionary     format=FLAT   ehrId=${ehr_id}     templateId=${template_id}
        Create Session      ${SUT}    ${ECISURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
    ELSE IF   '${format}'=='TDD'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/openehr.tds2+xml
        Set To Dictionary   ${headers}   Accept=application/openehr.tds2+xml
    ELSE IF   '${format}'=='STRUCTURED'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/openehr.wt.structured+json
        Set To Dictionary   ${headers}   Accept=application/openehr.wt.structured+json
    END

    IF          '${format}'=='FLAT'
        ${resp}     POST On Session     ${SUT}   composition   params=${params}
        ...     expected_status=anything   data=${file}   headers=${headers}
    ELSE
        ${resp}     POST On Session     ${SUT}   /ehr/${ehr_id}/composition
        ...     expected_status=anything   data=${file}   headers=${headers}
    END

    Set Suite Variable   ${response}     ${resp}
    Set Suite Variable   ${format}       ${format}
    Set Suite Variable   ${template}     ${template}

    capture point in time    1