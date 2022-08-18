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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#621-test-case-dv_parsable-open-constraint
...             ${\n}*6.2.1. Test case DV_PARSABLE open constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot

Suite Setup         Precondition
Suite Teardown      Delete Template Using API


*** Variables ***
${composition_file}     Test_dv_parsable_open_constraint.v0__.json
${optFile}              all_types/Test_dv_parsable_open_constraint.v0.opt


*** Test Cases ***
Composition With DV_PARSABLE.value NULL, DV_PARSABLE.formalism NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_PARSABLE open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_PARSABLE.value using ${dvParsableValue} argument value NULL
    ...     - update DV_PARSABLE.formalism using ${dvParsableFormalism} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PARSABLE Value And Formalism
    ...     ${NULL}    ${NULL}      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PARSABLE.value abc, DV_PARSABLE.formalism NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_PARSABLE open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_PARSABLE.value using ${dvParsableValue} argument value abc
    ...     - update DV_PARSABLE.formalism using ${dvParsableFormalism} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PARSABLE Value And Formalism
    ...     abc         ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PARSABLE.value NULL, DV_PARSABLE.formalism abc
    [Tags]      Negative
    [Documentation]     *Test case DV_PARSABLE open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_PARSABLE.value using ${dvParsableValue} argument value NULL
    ...     - update DV_PARSABLE.formalism using ${dvParsableFormalism} argument value abc
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PARSABLE Value And Formalism
    ...     ${NULL}     abc         ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PARSABLE.value xxx, DV_PARSABLE.formalism abc
    [Tags]      Positive
    [Documentation]     *Test case DV_PARSABLE open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_PARSABLE.value using ${dvParsableValue} argument value xxx
    ...     - update DV_PARSABLE.formalism using ${dvParsableFormalism} argument value abc
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_PARSABLE Value And Formalism
    ...     xxx         abc         ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API


*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_PARSABLE Value And Formalism
    [Arguments]     ${dvParsableValue}=xxx         ${dvParsableFormalism}=abc       ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvParsableValue}      ${dvParsableFormalism}
    commit composition      format=CANONICAL_JSON
    ...                     composition=${composition_file}
    ${isStatusCodeEqual}    Run Keyword And Return Status
    ...     Should Be Equal As Strings      ${response.status_code}     ${expectedCode}
    ${isUidPresent}     Run Keyword And Return Status
    ...     Set Test Variable   ${version_uid}    ${response.json()['uid']['value']}
    IF      ${isUidPresent} == ${TRUE}
        ${short_uid}        Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                            Set Suite Variable   ${versioned_object_uid}    ${short_uid}
    ELSE
        Set Suite Variable   ${versioned_object_uid}    ${None}
    END
    #Should Be Equal As Strings      ${response.status_code}     ${expectedCode}
    [Return]    ${isStatusCodeEqual}

Change Json KeyValue and Save Back To File
    [Documentation]     Updates DV_PARSABLE.value and DV_PARSABLE.formalism values
    ...     in Composition json, using arguments values.
    ...     Takes 3 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on DV_PARSABLE.value
    ...     3 - value to be on DV_PARSABLE.formalism
    [Arguments]     ${jsonContent}      ${dvParsableValueToUpdate}      ${dvParsableFormalismToUpdate}
    ${dvParsableValueJsonPath1}     Set Variable    content[0].data.events[0].data.items[0].value.value
    ${dvParsableValueJsonPath2}     Set Variable    content[0].data.events[1].data.items[0].value.value
    ${dvParsableValueJsonPath3}     Set Variable    content[0].data.events[2].data.items[0].value.value
    ${dvParsableFormalismJsonPath1}     Set Variable    content[0].data.events[0].data.items[0].value.formalism
    ${dvParsableFormalismJsonPath2}     Set Variable    content[0].data.events[1].data.items[0].value.formalism
    ${dvParsableFormalismJsonPath3}     Set Variable    content[0].data.events[2].data.items[0].value.formalism
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvParsableValueJsonPath1}
    ...             new_value=${dvParsableValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvParsableValueJsonPath2}
    ...             new_value=${dvParsableValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvParsableValueJsonPath3}
    ...             new_value=${dvParsableValueToUpdate}
    ####
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvParsableFormalismJsonPath1}
    ...             new_value=${dvParsableFormalismToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvParsableFormalismJsonPath2}
    ...             new_value=${dvParsableFormalismToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvParsableFormalismJsonPath3}
    ...             new_value=${dvParsableFormalismToUpdate}
    ${changedDvParsableValue1}   Get Value From Json     ${jsonContent}      ${dvParsableValueJsonPath1}
    ${changedDvParsableValue2}   Get Value From Json     ${jsonContent}      ${dvParsableValueJsonPath2}
    ${changedDvParsableValue3}   Get Value From Json     ${jsonContent}      ${dvParsableValueJsonPath3}
    ${changedDvParsableFormalism1}   Get Value From Json     ${jsonContent}      ${dvParsableFormalismJsonPath1}
    ${changedDvParsableFormalism2}   Get Value From Json     ${jsonContent}      ${dvParsableFormalismJsonPath2}
    ${changedDvParsableFormalism3}   Get Value From Json     ${jsonContent}      ${dvParsableFormalismJsonPath3}
    #Should Be Equal     ${changedDvDateTimeValue[0]}   ${valueToUpdate}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}

Load Json File With Composition
    [Documentation]     Loads Json content from composition file.
    ...     Stores file content in test variable, as well as full file path.
    ${COMPO DATA SETS}     Set Variable
    ...     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}compositions
    ${file}                 Get File   ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    ${compositionFilePath}  Set Variable    ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    Set Test Variable       ${file}
    Set Test Variable       ${compositionFilePath}

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