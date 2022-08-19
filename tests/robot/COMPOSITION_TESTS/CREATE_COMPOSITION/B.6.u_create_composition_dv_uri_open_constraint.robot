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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#721-test-case-dv_uri-open-constraint
...             ${\n}*7.2.1. Test case DV_URI open constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot

Suite Setup         Precondition
Suite Teardown      Delete Template Using API


*** Variables ***
${composition_file}     Test_dv_uri_open_constraint.v0__.json
${optFile}              all_types/Test_dv_uri_open_constraint.v0.opt


*** Test Cases ***
Composition With DV_URI.value NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_URI open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_URI.value using ${dvUriValue} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_URI Value
    ...     ${NULL}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_URI Value
    [Arguments]     ${dvUriValue}=http://www.ietf.org/rfc/rfc2396.txt         ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvUriValue}
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
    [Documentation]     Updates DV_URI.value value
    ...     in Composition json, using arguments values.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on DV_URI.value
    [Arguments]     ${jsonContent}      ${dvUriValueToUpdate}
    ${dvUriValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.value
    ${dvUriValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.value
    ${dvUriValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.value
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvUriValueJsonPath1}
    ...             new_value=${dvUriValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvUriValueJsonPath2}
    ...             new_value=${dvUriValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvUriValueJsonPath3}
    ...             new_value=${dvUriValueToUpdate}
    ${changedDvUriValue1}   Get Value From Json     ${jsonContent}      ${dvUriValueJsonPath1}
    ${changedDvUriValue2}   Get Value From Json     ${jsonContent}      ${dvUriValueJsonPath2}
    ${changedDvUriValue3}   Get Value From Json     ${jsonContent}      ${dvUriValueJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}