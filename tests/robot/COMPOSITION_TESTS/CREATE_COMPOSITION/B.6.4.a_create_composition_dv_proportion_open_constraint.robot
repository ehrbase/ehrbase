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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#361-test-case-dv_proportion-open-constraint-validate-rm-rules
...             ${\n}*3.6.1. Test case DV_PROPORTION open constraint, validate RM rules*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Test Cases ***
Composition With DV_PROPORTION Precision 0 Type Ratio - Numerator 10 - Denominator 500
    [Tags]      Positive
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Ratio (0) - Numerator 10 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 0,10,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    Set Suite Variable      ${composition_file}    Test_dv_proportion_open_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_proportion_open_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     0     10    500     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Ratio - Numerator 10 - Denominator 0
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Ratio (0) - Numerator 10 - Denominator 0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 0,10,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     0     10    0     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Valid_denominator failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Unitary - Numerator 10 - Denominator 1
    [Tags]      Positive
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Unitary (1) - Numerator 10 - Denominator 1 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 1,10,1
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=1     dvPropNumerator=10
    ...     dvPropDenominator=1     expectedCode=${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Unitary - Numerator 10 - Denominator 0
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Unitary (1) - Numerator 10 - Denominator 0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 1,10,0
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=1     dvPropNumerator=10
    ...     dvPropDenominator=0     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Valid_denominator failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Unitary - Numerator 10 - Denominator 500
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Unitary (1) - Numerator 10 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 1,10,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=1     dvPropNumerator=10
    ...     dvPropDenominator=500     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Unitary_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Percent - Numerator 10 - Denominator 0
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Percent (2) - Numerator 10 - Denominator 0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 2,10,0
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=2     dvPropNumerator=10
    ...     dvPropDenominator=0     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Valid_denominator failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Percent - Numerator 10 - Denominator 100
    [Tags]      Positive
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Percent (2) - Numerator 10 - Denominator 100 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 2,10,100
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=2     dvPropNumerator=10
    ...     dvPropDenominator=100     expectedCode=${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Percent - Numerator 10 - Denominator 500
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Percent (2) - Numerator 10 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 2,10,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=2     dvPropNumerator=10
    ...     dvPropDenominator=500     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Percent_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Fraction - Numerator 10 - Denominator 0
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Fraction (3) - Numerator 10 - Denominator 0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 3,10,0
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=3     dvPropNumerator=10
    ...     dvPropDenominator=0     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Valid_denominator failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Fraction - Numerator 10 - Denominator 100
    [Tags]      Positive
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Fraction (3) - Numerator 10 - Denominator 100 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 3,10,100
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=3     dvPropNumerator=10
    ...     dvPropDenominator=100     expectedCode=${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Integer Fraction - Numerator 10 - Denominator 0
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Integer Fraction (4) - Numerator 10 - Denominator 0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 4,10,0
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=4     dvPropNumerator=10
    ...     dvPropDenominator=0     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Valid_denominator failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Integer Fraction - Numerator 10 - Denominator 100
    [Tags]      Positive
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Integer Fraction (4) - Numerator 10 - Denominator 100 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 4,10,100
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=4     dvPropNumerator=10
    ...     dvPropDenominator=100     expectedCode=${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 0 Type Invalid - Numerator 10 - Denominator 500
    [Tags]      Negative
    [Documentation]     *Test case DV_PROPORTION Precision 0 Type Invalid (898) - Numerator 10 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 898,10,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=898     dvPropNumerator=10
    ...     dvPropDenominator=500     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   Invariant Type_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

Composition With DV_PROPORTION Precision 1 Type Fraction - Numerator 10 - Denominator 500
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_PROPORTION Precision 1 Type Fraction (3) - Numerator 10 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 3,10,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_proportion_open_constraint_precision_1.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_proportion_open_constraint_precision_1.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=3     dvPropNumerator=10
    ...     dvPropDenominator=500     expectedCode=${expectedStatusCode}
    #Log     ${response.json()["message"]}
    #Should Contain      ${response.json()["message"]}   Invariant Valid_denominator failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 1 Type Fraction - Numerator 10.5 - Denominator 500
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_PROPORTION Precision 1 Type Fraction (3) - Numerator 10.5 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 3,10.5,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=3     dvPropNumerator=10.5
    ...     dvPropDenominator=500     expectedCode=${expectedStatusCode}
    #Log     ${response.json()["message"]}
    Should Contain      ${response.json()["message"]}   ntegral_validity failed on type DV_PROPORTION
    Should Not Contain  ${response.json()["message"]}   Invariant Fraction_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 1 Type Fraction - Numerator 10 - Denominator 500.5
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_PROPORTION Precision 1 Type Fraction (3) - Numerator 10 - Denominator 500.5 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 3,10,500.5
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=3     dvPropNumerator=10
    ...     dvPropDenominator=500.5     expectedCode=${expectedStatusCode}
    Log     ${response.json()["message"]}
    Should Contain      ${response.json()["message"]}   ntegral_validity failed on type DV_PROPORTION
    Should Not Contain  ${response.json()["message"]}   Invariant Fraction_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 1 Type Integer Fraction - Numerator 10 - Denominator 500
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_PROPORTION Precision 1 Type Integer Fraction (4) - Numerator 10 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 4,10,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=4     dvPropNumerator=10
    ...     dvPropDenominator=500     expectedCode=${expectedStatusCode}
    #Should Contain      ${response.json()["message"]}   ntegral_validity failed on type DV_PROPORTION
    #Should Not Contain  ${response.json()["message"]}   Invariant Fraction_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 1 Type Integer Fraction - Numerator 10.5 - Denominator 500
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_PROPORTION Precision 1 Type Integer Fraction (4) - Numerator 10.5 - Denominator 500 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 4,10.5,500
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=4     dvPropNumerator=10.5
    ...     dvPropDenominator=500     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   ntegral_validity failed on type DV_PROPORTION
    Should Not Contain  ${response.json()["message"]}   Invariant Fraction_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_PROPORTION Precision 1 Type Integer Fraction - Numerator 10 - Denominator 500.5
    [Tags]      Negative    not-ready   bug
    [Documentation]     *Test case DV_PROPORTION Precision 1 Type Integer Fraction (4) - Numerator 10 - Denominator 500.5 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - ${dvPropType}, ${dvPropNumerator}, ${dvPropDenominator} arguments values 4,10,500.5
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_PROPORTION Values Open Constraint
    ...     dvPropType=4     dvPropNumerator=10
    ...     dvPropDenominator=500.5     expectedCode=${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   ntegral_validity failed on type DV_PROPORTION
    Should Not Contain  ${response.json()["message"]}   Invariant Fraction_validity failed on type DV_PROPORTION
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    #[Teardown]  Delete Composition Using API
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

#uncomment line 196

*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_PROPORTION Values Open Constraint
    [Arguments]     ${dvPropType}=0
    ...             ${dvPropNumerator}=10
    ...             ${dvPropDenominator}=500
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvPropType}  ${dvPropNumerator}   ${dvPropDenominator}
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
    [Documentation]     Updates DV_PROPORTION type, numerator, denominator
    ...     in Composition json, using arguments values.
    ...     Takes 4 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on dv_proportion.type
    ...     3 - value to be on dv_proportion.numerator
    ...     4 - value to be on dv_proportion.denominator
    [Arguments]     ${jsonContent}      ${typeValueToUpdate}   ${numeratorValueToUpdate}    ${denominatorValueToUpdate}
    ${typeValueJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.type
    ${numeratorValueJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.numerator
    ${denominatorValueJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.denominator
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${typeValueJsonPath}
    ...             new_value=${typeValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${numeratorValueJsonPath}
    ...             new_value=${numeratorValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${denominatorValueJsonPath}
    ...             new_value=${denominatorValueToUpdate}
    ${changedTypeValue}   Get Value From Json     ${jsonContent}      ${typeValueJsonPath}
    ${changedTypeValue}   Get Value From Json     ${jsonContent}      ${numeratorValueJsonPath}
    ${changedTypeValue}   Get Value From Json     ${jsonContent}      ${denominatorValueJsonPath}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}