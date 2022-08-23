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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#221-test-case-dv_text-with-open-constraint
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#223-test-case-dv_text-with-list-constraint
...             ${\n}*2.2.1. Test case DV_TEXT with open constraint*
...             ${\n}*2.2.3. Test case DV_TEXT with list constraint*
...             ${\n}*Without _2.2.2. Test case DV_TEXT with pattern constraint_ as C_STRING.pattern cannot be configured*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Test Cases ***
Composition With DV_TEXT.value NULL And DV_TEXT List Constraint NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_TEXT.value NULL:*
    ...     - DV_TEXT.value NULL
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_TEXT.value using ${dvTextValue} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_text_open_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_text_open_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_TEXT Value
    ...     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_TEXT.value ABC And DV_TEXT List Constraint NULL
    [Tags]      Positive
    [Documentation]     *Test case DV_TEXT.value ABC:*
    ...     - DV_TEXT.value ABC
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_TEXT.value using ${dvTextValue} argument value ABC
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_TEXT Value
    ...     ABC     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

Composition With DV_TEXT.value NULL And DV_TEXT List Constraint XYZ OPQ
    [Tags]      Negative
    [Documentation]     *Test case DV_TEXT.value NULL And DV_TEXT List Constraint:*
    ...     - DV_TEXT.value NULL And DV_TEXT List Constraint XYZ, OPQ
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_TEXT.value using ${dvTextValue} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_text_list_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_text_list_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_TEXT Value
    ...     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_TEXT.value ABC And DV_TEXT List Constraint XYZ OPQ
    [Tags]      Negative
    [Documentation]     *Test case DV_TEXT.value ABC And DV_TEXT List Constraint:*
    ...     - DV_TEXT.value ABC And DV_TEXT List Constraint XYZ, OPQ
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_TEXT.value using ${dvTextValue} argument value ABC
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_TEXT Value
    ...     ABC     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_TEXT.value OPQ And DV_TEXT List Constraint XYZ OPQ
    [Tags]      Positive
    [Documentation]     *Test case DV_TEXT.value OPQ And DV_TEXT List Constraint:*
    ...     - DV_TEXT.value OPQ And DV_TEXT List Constraint XYZ, OPQ
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_TEXT.value using ${dvTextValue} argument value OPQ
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_TEXT Value
    ...     OPQ     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API


*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_TEXT Value
    [Arguments]     ${dvTextValue}=ABC
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvTextValue}
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
    [Documentation]     Updates DV_TEXT.value
    ...     in Composition json, using arguments values.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on DV_TEXT.value
    [Arguments]     ${jsonContent}      ${dvTextValueToUpdate}
    ${dvTextValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.value
    ${dvTextValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.value
    ${dvTextValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.value
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvTextValueJsonPath1}
    ...             new_value=${dvTextValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvTextValueJsonPath2}
    ...             new_value=${dvTextValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvTextValueJsonPath3}
    ...             new_value=${dvTextValueToUpdate}
    ${changedDvTextValue1}   Get Value From Json     ${jsonContent}      ${dvTextValueJsonPath1}
    ${changedDvTextValue2}   Get Value From Json     ${jsonContent}      ${dvTextValueJsonPath2}
    ${changedDvTextValue3}   Get Value From Json     ${jsonContent}      ${dvTextValueJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}