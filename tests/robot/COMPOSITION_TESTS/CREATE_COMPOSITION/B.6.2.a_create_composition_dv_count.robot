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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#341-test-case-dv_count-open-constraint
...             ${\n}*3.4.1. Test case DV_COUNT open constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Test Cases ***
Composition With DV_COUNT Magnitude NULL Open Constraint
    [Tags]      Negative
    [Documentation]     *Test case DV_COUNT Magnitude NULL Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_count_open_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_count_open_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 0 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_COUNT Magnitude 0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${0}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 1 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_COUNT Magnitude 1 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 1
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${1}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 15 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_COUNT Magnitude 15 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 15
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${15}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 30 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_COUNT Magnitude 30 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 30
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${30}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

Composition With DV_COUNT Magnitude NULL Range Constraint
    [Tags]      Negative
    [Documentation]     *Test case DV_COUNT Magnitude NULL Range Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_count_range_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_count_range_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 0 Range Constraint
    [Tags]      Negative
    [Documentation]     *Test case DV_COUNT Magnitude 0 Range Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${0}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 10 Range Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_COUNT Magnitude 10 Range Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 10
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${10}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 19 Range Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_COUNT Magnitude 19 Range Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 19
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${19}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 20 Range Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_COUNT Magnitude 20 Range Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 20
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${20}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_COUNT Magnitude 25 Range Constraint
    [Tags]      Negative
    [Documentation]     *Test case DV_COUNT Magnitude 25 Range Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_COUNT Magnitude using:
    ...     - ${dvCountMagnitude} argument value 25
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_COUNT Magnitude Value
    ...     ${25}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_COUNT Magnitude Value
    [Arguments]     ${dvCountMagnitude}=15
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvCountMagnitude}
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
    [Documentation]     Updates DV_COUNT Magnitude
    ...     in Composition json, using arguments values.
    ...     Takes 2 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on dv_count.value.magnitude
    [Arguments]     ${jsonContent}      ${magnitudeValueToUpdate}
    ${magnitudeValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.magnitude
    ${magnitudeValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.magnitude
    ${magnitudeValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.magnitude
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${magnitudeValueJsonPath1}
    ...             new_value=${magnitudeValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${magnitudeValueJsonPath2}
    ...             new_value=${magnitudeValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${magnitudeValueJsonPath3}
    ...             new_value=${magnitudeValueToUpdate}
    ${changedMagnitudeValue1}   Get Value From Json     ${jsonContent}      ${magnitudeValueJsonPath1}
    ${changedMagnitudeValue2}   Get Value From Json     ${jsonContent}      ${magnitudeValueJsonPath2}
    ${changedMagnitudeValue3}   Get Value From Json     ${jsonContent}      ${magnitudeValueJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}