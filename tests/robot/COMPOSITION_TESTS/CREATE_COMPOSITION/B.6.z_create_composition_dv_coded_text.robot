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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#231-test-case-dv_coded_text-with-open-constraint
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#232-test-case-dv_coded_text-with-local-codes
...             ${\n}*2.3.1. Test case DV_CODED_TEXT with open constraint*
...             ${\n}*2.3.2. Test case DV_CODED_TEXT with local codes*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Test Cases ***
Composition With DV_CODED_TEXT Code_String And Terminology_id NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_CODED_TEXT Code_String And Terminology_id NULL:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_coded_text_open_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_coded_text_open_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     ${NULL}     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_CODED_TEXT Code_String 42 And Terminology_id NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_CODED_TEXT Code_String 42 And Terminology_id NULL:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments values 42, NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     42     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_CODED_TEXT Code_String NULL And Terminology_id Local
    [Tags]      Negative
    [Documentation]     *Test case DV_CODED_TEXT Code_String NULL And Terminology_id local:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments values NULL, local
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     ${NULL}     local     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_CODED_TEXT Code_String 42 And Terminology_id Local
    [Tags]      Positive
    [Documentation]     *Test case DV_CODED_TEXT Code_String 42 And Terminology_id local:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments values 42, local
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     42     local     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

Composition With DV_CODED_TEXT Code_String And Terminology_id NULL With Configured Local Codes
    [Tags]      Negative
    [Documentation]     *Test case DV_CODED_TEXT Code_String And Terminology_id NULL with configured local codes:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_coded_text_with_local_codes.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_coded_text_with_local_codes.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     ${NULL}     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_CODED_TEXT Code_String ABC And Terminology_id NULL With Configured Local Codes
    [Tags]      Negative
    [Documentation]     *Test case DV_CODED_TEXT Code_String ABC And Terminology_id NULL with configured local codes:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments values ABC, NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     ABC     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_CODED_TEXT Code_String NULL And Terminology_id Local With Configured Local Codes
    [Tags]      Negative
    [Documentation]     *Test case DV_CODED_TEXT Code_String NULL And Terminology_id local with configured local codes:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments values NULL, local
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     ${NULL}     local     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_CODED_TEXT Code_String at0035 And Terminology_id Local With Configured Local Codes
    [Tags]      Positive
    [Documentation]     *Test case DV_CODED_TEXT Code_String at0035 And Terminology_id local with configured local codes:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_CODED_TEXT Code_String And Terminology_id using:
    ...     - ${codeStringValue}, ${terminologyIdValue} arguments values at0035, local
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified Code_String And Terminology_Id Value
    ...     at0035     local     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API


*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified Code_String And Terminology_Id Value
    [Arguments]     ${codeStringValue}=42
    ...             ${terminologyIdValue}=local
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${codeStringValue}      ${terminologyIdValue}
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
    [Documentation]     Updates DV_CODED_TEXT Code_String And Terminology_id
    ...     in Composition json, using arguments values.
    ...     Takes 3 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on defining_code.code_string
    ...     3 - value to be on terminology_id.value
    [Arguments]     ${jsonContent}      ${codeStringValueToUpdate}      ${terminologyIdValueToUpdate}
    ${codeStringValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.defining_code.code_string
    ${codeStringValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.defining_code.code_string
    ${codeStringValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.defining_code.code_string
    ${terminologyIdValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.defining_code.terminology_id.value
    ${terminologyIdValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.defining_code.terminology_id.value
    ${terminologyIdValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.defining_code.terminology_id.value
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${codeStringValueJsonPath1}
    ...             new_value=${codeStringValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${codeStringValueJsonPath2}
    ...             new_value=${codeStringValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${codeStringValueJsonPath3}
    ...             new_value=${codeStringValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${terminologyIdValueJsonPath1}
    ...             new_value=${terminologyIdValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${terminologyIdValueJsonPath2}
    ...             new_value=${terminologyIdValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${terminologyIdValueJsonPath3}
    ...             new_value=${terminologyIdValueToUpdate}
    ${changedCodeStringValue1}   Get Value From Json     ${jsonContent}      ${codeStringValueJsonPath1}
    ${changedCodeStringValue2}   Get Value From Json     ${jsonContent}      ${codeStringValueJsonPath2}
    ${changedCodeStringValue3}   Get Value From Json     ${jsonContent}      ${codeStringValueJsonPath3}
    ${changedTerminologyIdValue1}   Get Value From Json     ${jsonContent}      ${terminologyIdValueJsonPath1}
    ${changedTerminologyIdValue2}   Get Value From Json     ${jsonContent}      ${terminologyIdValueJsonPath2}
    ${changedTerminologyIdValue3}   Get Value From Json     ${jsonContent}      ${terminologyIdValueJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}