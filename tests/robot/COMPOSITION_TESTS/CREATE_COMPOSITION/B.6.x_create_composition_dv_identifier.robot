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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#131-test-case-validating-all-attributes-using-the-pattern-constraint
...             ${\n}*1.3.1 DV_IDENTIFIER Test case validating all attributes using the pattern constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot

Suite Setup         Precondition
Suite Teardown      Delete Template Using API


*** Variables ***
${composition_file}     Test_dv_identifier_pattern_constraint.v0__.json
${optFile}              all_types/Test_dv_identifier_pattern_constraint.v0.opt


*** Test Cases ***
Composition With DV_IDENTIFIER.issuer NULL
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_IDENTIFIER.issuer NULL:*
    ...     - DV_IDENTIFIER.issuer value NULL
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.issuer using ${dvIdentifierValue} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    issuer
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ${NULL}     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}     AND
    ...         TRACE JIRA ISSUE    CDR-519

Composition With DV_IDENTIFIER.issuer ABC
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_IDENTIFIER.issuer ABC:*
    ...     - DV_IDENTIFIER.issuer value ABC
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.issuer using ${dvIdentifierValue} argument value ABC
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    issuer
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ABC     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}     AND
    ...         TRACE JIRA ISSUE    CDR-519

Composition With DV_IDENTIFIER.issuer XYZ
    [Tags]      Positive
    [Documentation]     *Test case DV_IDENTIFIER.issuer XYZ:*
    ...     - DV_IDENTIFIER.issuer value XYZ
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.issuer using ${dvIdentifierValue} argument value XYZ
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}           Set Variable    201
    ${idenfitierKeyToBeChanged}     Set Variable    issuer
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     XYZ     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}

Composition With DV_IDENTIFIER.assigner NULL
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_IDENTIFIER.assigner NULL:*
    ...     - DV_IDENTIFIER.assigner value NULL
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.assigner using ${dvIdentifierValue} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    assigner
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ${NULL}     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}     AND
    ...         TRACE JIRA ISSUE    CDR-519

Composition With DV_IDENTIFIER.assigner ABC
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_IDENTIFIER.assigner ABC:*
    ...     - DV_IDENTIFIER.assigner value ABC
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.assigner using ${dvIdentifierValue} argument value ABC
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    assigner
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ABC     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}     AND
    ...         TRACE JIRA ISSUE    CDR-519

Composition With DV_IDENTIFIER.assigner XYZ
    [Tags]      Positive
    [Documentation]     *Test case DV_IDENTIFIER.assigner XYZ:*
    ...     - DV_IDENTIFIER.assigner value XYZ
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.assigner using ${dvIdentifierValue} argument value XYZ
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}           Set Variable    201
    ${idenfitierKeyToBeChanged}     Set Variable    assigner
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     XYZ     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}

Composition With DV_IDENTIFIER.id NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_IDENTIFIER.id NULL:*
    ...     - DV_IDENTIFIER.id value NULL
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.id using ${dvIdentifierValue} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    id
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ${NULL}     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}

Composition With DV_IDENTIFIER.id ABC
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_IDENTIFIER.id ABC:*
    ...     - DV_IDENTIFIER.id value ABC
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.id using ${dvIdentifierValue} argument value ABC
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    id
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ABC     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}     AND
    ...         TRACE JIRA ISSUE    CDR-519

Composition With DV_IDENTIFIER.id XYZ
    [Tags]      Positive
    [Documentation]     *Test case DV_IDENTIFIER.id XYZ:*
    ...     - DV_IDENTIFIER.id value XYZ
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.id using ${dvIdentifierValue} argument value XYZ
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}           Set Variable    201
    ${idenfitierKeyToBeChanged}     Set Variable    id
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     XYZ     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}

Composition With DV_IDENTIFIER.type NULL
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_IDENTIFIER.type NULL:*
    ...     - DV_IDENTIFIER.type value NULL
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.type using ${dvIdentifierValue} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    type
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ${NULL}     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}     AND
    ...         TRACE JIRA ISSUE    CDR-519

Composition With DV_IDENTIFIER.type ABC
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_IDENTIFIER.type ABC:*
    ...     - DV_IDENTIFIER.type value ABC
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.type using ${dvIdentifierValue} argument value ABC
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}           Set Variable    422
    ${idenfitierKeyToBeChanged}     Set Variable    type
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     ABC     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}     AND
    ...         TRACE JIRA ISSUE    CDR-519

Composition With DV_IDENTIFIER.type XYZ
    [Tags]      Positive
    [Documentation]     *Test case DV_IDENTIFIER.type XYZ:*
    ...     - DV_IDENTIFIER.type value XYZ
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_IDENTIFIER.type using ${dvIdentifierValue} argument value XYZ
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}           Set Variable    201
    ${idenfitierKeyToBeChanged}     Set Variable    type
    ${statusCodeBoolean}    Commit Composition With Modified DV_IDENTIFIER Value
    ...     XYZ     ${idenfitierKeyToBeChanged}    ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND
    ...         Restore Initial Version Of Composition DV_IDENTIFIER Values     ${idenfitierKeyToBeChanged}


*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_IDENTIFIER Value
    [Arguments]     ${dvIdentifierValue}=XYZ
    ...             ${idenfitierKeyToBeChanged}=issuer
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvIdentifierValue}    ${idenfitierKeyToBeChanged}
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
    [Documentation]     Updates DV_IDENTIFIER.{key} value
    ...     in Composition json, using arguments values.
    ...     Takes 3 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on DV_IDENTIFIER.{key}
    ...     3 - {key} where value will be updated
    [Arguments]     ${jsonContent}      ${dvIdentifierValueToUpdate}    ${dvIdentifierKeyWhereToUpdate}
    ${dvIdentifierValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.${dvIdentifierKeyWhereToUpdate}
    ${dvIdentifierValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.${dvIdentifierKeyWhereToUpdate}
    ${dvIdentifierValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.${dvIdentifierKeyWhereToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvIdentifierValueJsonPath1}
    ...             new_value=${dvIdentifierValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvIdentifierValueJsonPath2}
    ...             new_value=${dvIdentifierValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvIdentifierValueJsonPath3}
    ...             new_value=${dvIdentifierValueToUpdate}
    ${changedDvIdentifierValue1}   Get Value From Json     ${jsonContent}      ${dvIdentifierValueJsonPath1}
    ${changedDvIdentifierValue2}   Get Value From Json     ${jsonContent}      ${dvIdentifierValueJsonPath2}
    ${changedDvIdentifierValue3}   Get Value From Json     ${jsonContent}      ${dvIdentifierValueJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}

Restore Initial Version Of Composition DV_IDENTIFIER Values
    [Arguments]     ${valueToBeRestoredInKey}=id
    Log     ${valueToBeRestoredInKey}
    #${keysList}     Create List     id      issuer      type    assigner
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   XYZ    ${valueToBeRestoredInKey}