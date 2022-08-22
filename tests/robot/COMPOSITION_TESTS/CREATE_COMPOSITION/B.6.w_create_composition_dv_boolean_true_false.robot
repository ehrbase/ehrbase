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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#122-test-case-only-true-allowed
...             ${\n}*1.2.2 DV_BOOLEAN Test case only true allowed*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot

Suite Setup         Precondition
Suite Teardown      Delete Template Using API


*** Variables ***
${composition_file}     Test_dv_boolean_true_false.v0__.json
${optFile}              all_types/Test_dv_boolean_true_false.v0.opt


*** Test Cases ***
Composition With DV_BOOLEAN.value True - C_BOOLEAN True Valid - False Invalid
    [Tags]      Positive
    [Documentation]     *Test case DV_BOOLEAN:*
    ...     - C_BOOLEAN.true_valid = true
    ...     - C_BOOLEAN.false_valid = false
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_BOOLEAN.value using ${dvBooleanValue} argument value true
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_BOOLEAN Value
    ...     ${TRUE}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_BOOLEAN.value False - C_BOOLEAN True Valid - False Invalid
    [Tags]      Negative
    [Documentation]     *Test case DV_BOOLEAN:*
    ...     - C_BOOLEAN.true_valid = true
    ...     - C_BOOLEAN.false_valid = false
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_BOOLEAN.value using ${dvBooleanValue} argument value false
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_BOOLEAN Value
    ...     ${FALSE}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API


*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_BOOLEAN Value
    [Arguments]     ${dvBooleanValue}=${TRUE}
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvBooleanValue}
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
    [Return]    ${isStatusCodeEqual}

Change Json KeyValue and Save Back To File
    [Documentation]     Updates DV_BOOLEAN.value value
    ...     in Composition json, using arguments values.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on DV_BOOLEAN.value
    [Arguments]     ${jsonContent}      ${dvBooleanValueToUpdate}
    ${dvBooleanValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.value
    ${dvBooleanValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.value
    ${dvBooleanValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.value
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvBooleanValueJsonPath1}
    ...             new_value=${dvBooleanValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvBooleanValueJsonPath2}
    ...             new_value=${dvBooleanValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvBooleanValueJsonPath3}
    ...             new_value=${dvBooleanValueToUpdate}
    ${changedDvBooleanValue1}   Get Value From Json     ${jsonContent}      ${dvBooleanValueJsonPath1}
    ${changedDvBooleanValue2}   Get Value From Json     ${jsonContent}      ${dvBooleanValueJsonPath2}
    ${changedDvBooleanValue3}   Get Value From Json     ${jsonContent}      ${dvBooleanValueJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}