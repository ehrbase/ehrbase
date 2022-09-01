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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#351-test-case-dv_quantity-open-constraint
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#352-test-case-dv_quantity-only-property-is-constrained
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#353-test-case-dv_quantity-property-and-units-are-constrained-without-magnitude-range
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#354-test-case-dv_quantity-property-and-units-are-constrained-with-magnitude-range
...             ${\n}*3.5.1. Test case DV_QUANTITY open constraint*
...             ${\n}*3.5.2. Test case DV_QUANTITY only property is constrained*
...             ${\n}*3.5.3. Test case DV_QUANTITY property and units are constrained, without magnitude range*
...             ${\n}*3.5.4. Test case DV_QUANTITY property and units are constrained, with magnitude range*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Test Cases ***
Composition With DV_QUANTITY Units And Magnitude NULL Open Constraint
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units And Magnitude NULL Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_quantity_open_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_quantity_open_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     ${NULL}     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude NULL Open Constraint
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude NULL Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments values cm, NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units NULL And Magnitude 1.0 Open Constraint
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units NULL And Magnitude 1.0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments values NULL, 1.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     ${NULL}     1.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 0.0 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 0.0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments values cm, 0.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     0.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 1.0 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 1.0 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments values cm, 1.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     1.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 5.7 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 5.7 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments values cm, 5.7
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     5.7     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units mm And Magnitude 22 Open Constraint
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units mm And Magnitude 22 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments values mm, 22
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     mm     22     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API


Composition With DV_QUANTITY Units mmHg And Magnitude 130 Open Constraint High Blood Pressure
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units mmHg And Magnitude 130 Open Constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments values mmHg, 130
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     mmHg     130     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

Composition With DV_QUANTITY Units cm And Magnitude NULL Property Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude NULL Property Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm, NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_quantity_property_constrained.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_quantity_property_constrained.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units NULL And Magnitude 1.0 Property Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units NULL And Magnitude 1.0 Property Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value NULL, 1.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     ${NULL}     1.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units mg And Magnitude 0.0 Property Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units mg And Magnitude 0.0 Property Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value mg, 0.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     mg     0.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 0.0 Property Constrained
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 0.0 Property Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm, 0.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     0.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 8.7 Property Constrained
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 8.7 Property Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm, 8.7
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     8.7     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

Composition With DV_QUANTITY Units And Magnitude NULL Property And Units Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units And Magnitude NULL Property And Units Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_dv_quantity_property_units_constrained.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_dv_quantity_property_units_constrained.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     ${NULL}     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude NULL Property And Units Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude NULL Property And Units Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm, NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units NULL And Magnitude 1.0 Property And Units Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units NULL And Magnitude 1.0 Property And Units Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value NULL, 1.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     ${NULL}     1.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units mg And Magnitude 0.0 Property And Units Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units mg And Magnitude 0.0 Property And Units Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value mg, 0.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     mg     0.0     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units km And Magnitude 2 Property And Units Constrained
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units km And Magnitude 2 Property And Units Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value km, 2
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     km     2     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units m And Magnitude 2.5 Property And Units Constrained
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units m And Magnitude 2.5 Property And Units Constrained:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value m, 2.5
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     m     2.5     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API

Composition With DV_QUANTITY Units And Magnitude NULL Property And Units Constrained With Magnitude Range
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units And Magnitude NULL Property And Units Constrained With Magnitude Range:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}
    ...         Test_dv_quantity_property_units_constrained_with_magnitude_range.v0__.json
    Set Suite Variable      ${optFile}
    ...         all_types/Test_dv_quantity_property_units_constrained_with_magnitude_range.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     ${NULL}     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude NULL Property And Units Constrained With Magnitude Range
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude NULL Property And Units Constrained With Magnitude Range:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm,NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     ${NULL}     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 0.0 Property And Units Constrained With Magnitude Range
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 0.0 Property And Units Constrained With Magnitude Range:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm,0.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     0.0      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units km And Magnitude 0.0 Property And Units Constrained With Magnitude Range
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units km And Magnitude 0.0 Property And Units Constrained With Magnitude Range:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value km,0.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     km     0.0      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 1.0 Property And Units Constrained With Magnitude Range
    [Tags]      Negative
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 1.0 Property And Units Constrained With Magnitude Range:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm,1.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     1.0      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 5.0 Property And Units Constrained With Magnitude Range
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 5.0 Property And Units Constrained With Magnitude Range:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm,5.0
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     5.0      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_QUANTITY Units cm And Magnitude 6.3 Property And Units Constrained With Magnitude Range
    [Tags]      Positive
    [Documentation]     *Test case DV_QUANTITY Units cm And Magnitude 6.3 Property And Units Constrained With Magnitude Range:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_QUANTITY Units And Magnitude using:
    ...     - ${dvQuantityUnits}, ${dvQuantityMagnitude} arguments value cm,6.3
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    ...     cm     6.3      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Run Keywords    Delete Composition Using API    AND     Delete Template Using API


*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_QUANTITY Units And Magnitude Values
    [Arguments]     ${dvQuantityUnits}=mm
    ...             ${dvQuantityMagnitude}=22
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvQuantityUnits}  ${dvQuantityMagnitude}
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
    [Documentation]     Updates DV_QUANTITY Units And Magnitude
    ...     in Composition json, using arguments values.
    ...     Takes 3 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on dv_quantity.units
    ...     3 - value to be on dv_quantity.magnitude
    [Arguments]     ${jsonContent}      ${unitsValueToUpdate}   ${magnitudeValueToUpdate}
    ${unitsValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.units
    ${unitsValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.units
    ${unitsValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.units
    ${magnitudeValueJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.magnitude
    ${magnitudeValueJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.magnitude
    ${magnitudeValueJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.magnitude
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${unitsValueJsonPath1}
    ...             new_value=${unitsValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${unitsValueJsonPath2}
    ...             new_value=${unitsValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${unitsValueJsonPath3}
    ...             new_value=${unitsValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${magnitudeValueJsonPath1}
    ...             new_value=${magnitudeValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${magnitudeValueJsonPath2}
    ...             new_value=${magnitudeValueToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${magnitudeValueJsonPath3}
    ...             new_value=${magnitudeValueToUpdate}
    ${changedUnitsValue1}   Get Value From Json     ${jsonContent}      ${unitsValueJsonPath1}
    ${changedUnitsValue2}   Get Value From Json     ${jsonContent}      ${unitsValueJsonPath2}
    ${changedUnitsValue3}   Get Value From Json     ${jsonContent}      ${unitsValueJsonPath3}
    ${changedMagnitudeValue1}   Get Value From Json     ${jsonContent}      ${magnitudeValueJsonPath1}
    ${changedMagnitudeValue2}   Get Value From Json     ${jsonContent}      ${magnitudeValueJsonPath2}
    ${changedMagnitudeValue3}   Get Value From Json     ${jsonContent}      ${magnitudeValueJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}