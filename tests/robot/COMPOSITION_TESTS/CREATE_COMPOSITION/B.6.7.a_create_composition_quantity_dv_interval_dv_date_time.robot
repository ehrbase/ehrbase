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
...             \nBased on:
...             - https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#391-test-case-dv_intervaldv_date_time-open-constraint
...             \n*3.9.1. Test case DV_INTERVAL<DV_DATE_TIME> open constraint*
...             \nIn tests name (applicable for lower_unb, upper_unb, lower_incl, upper_incl):
...             - 1 is True
...             - 0 is False
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/admin_keywords.robot
Resource        ../../_resources/suite_settings.robot


*** Test Cases ***
3.9.1. Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower Null - Upper Null - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative    not-ready   bug
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower Null, Upper Null, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*T
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    Set Suite Variable      ${composition_file}    Test_quantity_dv_interval_dv_date_time_open_constraint.v0__.json
    Set Suite Variable      ${optFile}             all_types/Test_quantity_dv_interval_dv_date_time_open_constraint.v0.opt
    Precondition
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     ${NULL}     ${NULL}
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...     /value/interval/lower/value: Attribute value of class DV_DATE_TIME does not match existence 1..1
    Should Contain      ${response.json()["message"]}
    ...     /value/interval/upper/value: Attribute value of class DV_DATE_TIME does not match existence 1..1
    Should Not Contain      ${response.json()["message"]}   NullPointerException
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Run Keywords     Delete Composition Using API    AND     TRACE JIRA ISSUE    CDR-540

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower Null - Upper Empty - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower Null, Upper Empty, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     ${NULL}     ${EMPTY}
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...     com.nedap.archie.rm.datavalues.quantity.DvInterval["upper"]->com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime["value"]
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower Empty - Upper Null - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower Empty, Upper Null, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     ${EMPTY}     ${NULL}
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...     com.nedap.archie.rm.datavalues.quantity.DvInterval["lower"]->com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime["value"]
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021 - Upper Null - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative    not-ready   bug
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021, Upper Null, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021     ${NULL}
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...     /value/interval/upper/value: Attribute value of class DV_DATE_TIME does not match existence 1..1
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower Null - Upper 2022 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative    not-ready   bug
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower Null, Upper 2022, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     ${NULL}     2022
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...     /value/interval/lower/value: Attribute value of class DV_DATE_TIME does not match existence 1..1
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021 - Upper 2022 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive    not-ready   bug
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021, Upper 2022, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021    2022
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Run Keywords     Delete Composition Using API    AND     TRACE JIRA ISSUE    CDR-541

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-00 - Upper 2022-01 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative    not-ready   bug
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-00, Upper 2022-01, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-00    2022-01
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Log     ${response.json()["message"]}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Run Keywords     Delete Composition Using API    AND     TRACE JIRA ISSUE    CDR-542

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-01 - Upper 2022-01 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive    not-ready   bug
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-01, Upper 2022-01, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-01    2022-01
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Log     ${response.json()["message"]}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Run Keywords     Delete Composition Using API    AND     TRACE JIRA ISSUE    CDR-543

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-01-00 - Upper 2022-01-01 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-01-00, Upper 2022-01-01, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-01-00    2022-01-01
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...      could not be parsed at index 4:2021-01-00
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-01-32 - Upper 2022-01-01 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-01-32, Upper 2022-01-01, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-01-32    2022-01-01
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...      could not be parsed at index 4:2021-01-32
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-01-01 - Upper 2022-01-00 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-01-01, Upper 2022-01-00, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-01-01    2022-01-00
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...      could not be parsed at index 4:2022-01-00
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-01-30 - Upper 2022-01-00 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-01-30, Upper 2022-01-00, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-01-30    2022-01-00
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}
    ...      could not be parsed at index 4:2022-01-00
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-01-28 - Upper 2022-01-15 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive    not-ready   bug
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-01-28, Upper 2022-01-15, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-01-28    2022-01-15
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Log     ${response.json()["message"]}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Run Keywords     Delete Composition Using API    AND     TRACE JIRA ISSUE    CDR-544

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T48 - Upper 2022-01-15T10 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T48, Upper 2022-01-15T10, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T48    2022-01-15T10
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   could not be parsed at index 4:2021-10-24T4
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T21 - Upper 2022-01-15T73 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T21, Upper 2022-01-15T73, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T21    2022-01-15T73
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain      ${response.json()["message"]}   could not be parsed at index 4:2022-01-15T73
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T05 - Upper 2022-01-15T10 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T05, Upper 2022-01-15T10, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T05    2022-01-15T10
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T05:30 - Upper 2022-01-15T10:61 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T05:30, Upper 2022-01-15T10:61, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T05:30    2022-01-15T10:61
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain     ${response.json()["message"]}
    ...     could not be parsed at index 4:2022-01-15T10:61
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T05:30 - Upper 2022-01-15T10:45 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T05:30, Upper 2022-01-15T10:45, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T05:30    2022-01-15T10:45
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T05:30:78 - Upper 2022-01-15T10:45:13 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Negative
    [Documentation]     Negative case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T05:30:78, Upper 2022-01-15T10:45:13, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 400.
    ${expectedStatusCode}   Set Variable    400
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T05:30:78    2022-01-15T10:45:13
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    Should Contain     ${response.json()["message"]}
    ...     could not be parsed at index 4:2021-10-24T05:30:78
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T05:30:47.5 - Upper 2022-01-15T10:45:13.6 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T05:30:47.5, Upper 2022-01-15T10:45:13.6, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T05:30:47.5    2022-01-15T10:45:13.6
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T05:30:47.333333 - Upper 2022-01-15T10:45:13.555555 - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T05:30:47.333333, Upper 2022-01-15T10:45:13.555555, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T05:30:47.333333      2022-01-15T10:45:13.555555
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]     Delete Composition Using API

Test QUANTITY DV_INTERVAL<DV_DATE_TIME> Lower 2021-10-24T05:30:47Z - Upper 2022-01-15T10:45:13Z - Lower Unb 0 - Upper Unb 0 - Lower Incl 1 - Upper Incl 1
    [Tags]      Positive
    [Documentation]     Positive case for QUANTITY DV_INTERVAL<DV_DATE_TIME>
    ...     Lower 2021-10-24T05:30:47Z, Upper 2022-01-15T10:45:13Z, Lower Unb 0, Upper Unb 0, Lower Incl 1, Upper Incl 1
    ...     \n*See suite documentation to understand what are 1 and 0 values!*
    ...     - load json file from CANONICAL_JSON folder
    ...     - set ${dvLower}, ${dvUpper}, ${dvLowerUnb}, ${dvUpperUnb}, ${dvLowerIncl}, ${dvUpperIncl}
    ...     - commit composition\n- check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    ...     2021-10-24T05:30:47Z      2022-01-15T10:45:13Z
    ...     ${FALSE}     ${FALSE}     ${TRUE}    ${TRUE}
    ...     ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    #[Teardown]     Delete Composition Using API
    [Teardown]     Run Keywords     Delete Composition Using API    AND     Delete Template Using API


*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified QUANTITY DV_INTERVAL<DV_DATE_TIME> Values
    [Arguments]     ${dvLower}=1
    ...             ${dvUpper}=10
    ...             ${dvLowerUnb}=${FALSE}
    ...             ${dvUpperUnb}=${FALSE}
    ...             ${dvLowerIncl}=${TRUE}
    ...             ${dvUpperIncl}=${TRUE}
    ...             ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvLower}  ${dvUpper}
    ...     ${dvLowerUnb}   ${dvUpperUnb}   ${dvLowerIncl}  ${dvUpperIncl}
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
    [Documentation]     Updates DV_INTERVAL<DV_COUNT> lower, upper, lower_unb, upper_unb, lower_incl, upper_incl
    ...     in Composition json, using arguments values.
    ...     Takes 7 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on dv_interval.lower.value
    ...     3 - value to be on dv_interval.upper.value
    ...     4 - value to be on dv_interval.lower_unbounded
    ...     5 - value to be on dv_interval.upper_unbounded
    ...     6 - value to be on dv_interval.lower_included
    ...     7 - value to be on dv_interval.upper_included
    [Arguments]     ${jsonContent}      ${dvLowerToUpdate}      ${dvUpperToUpdate}
    ...     ${dvLowerUnbToUpdate}    ${dvUpperUnbToUpdate}      ${dvLowerInclToUpdate}
    ...     ${dvUpperInclToUpdate}
    ${dvLowerJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.lower.value
    ${dvUpperJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.upper.value
    ${dvLowerUnbJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.lower_unbounded
    ${dvUpperUnbJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.upper_unbounded
    ${dvLowerInclJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.lower_included
    ${dvUpperInclJsonPath}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.upper_included
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvLowerJsonPath}
    ...             new_value=${dvLowerToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvUpperJsonPath}
    ...             new_value=${dvUpperToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvLowerUnbJsonPath}
    ...             new_value=${dvLowerUnbToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvUpperUnbJsonPath}
    ...             new_value=${dvUpperUnbToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvLowerInclJsonPath}
    ...             new_value=${dvLowerInclToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvUpperInclJsonPath}
    ...             new_value=${dvUpperInclToUpdate}
    ${changedDvLower}   Get Value From Json     ${jsonContent}      ${dvLowerJsonPath}
    ${changedDvUpper}   Get Value From Json     ${jsonContent}      ${dvUpperJsonPath}
    ${changedDvLowerUnb}   Get Value From Json      ${jsonContent}      ${dvLowerUnbJsonPath}
    ${changedDvUpperUnb}   Get Value From Json      ${jsonContent}      ${dvUpperUnbJsonPath}
    ${changedDvLowerIncl}   Get Value From Json     ${jsonContent}      ${dvLowerInclJsonPath}
    ${changedDvUpperIncl}   Get Value From Json     ${jsonContent}      ${dvUpperInclJsonPath}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}