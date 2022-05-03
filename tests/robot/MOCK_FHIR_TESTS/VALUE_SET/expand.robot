*** Settings ***
Documentation   ValueSet expand operation coverage by Robot Test Suite, using Mockup Server
...
Metadata        TOP_TEST_SUITE    MOCK_FHIR_TESTS

Resource        ../../_resources/keywords/mock_fhir_value_set_keywords.robot
Suite Setup     Create Sessions
Suite Teardown  Reset Mock Server


*** Variables ***
${MOCK_VALUE_SET_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/mocks/value_set


*** Test Cases ***
Value Set GET Using Expand Operation - Positive
    [Documentation]     Get ValueSet using expand operation,
    ...     with valid:
    ...     - url, count, includeDesignations, activeOnly values.
    ...     Positive flow.
    [Tags]      Mock    VALUESET      Positive      Expand
    GET Create Mock Expectation - ValueSet - Expand Operation
    ...     ${MOCK_VALUE_SET_PATH}/expand_success.json
    ...     None    200
    Send GET - ValueSet
    ...     endpoint=${VALUE_SET_EXPAND_ENDPOINT}   statusCode=200
    Should Be Equal As Strings      ${response["expansion"]["parameter"][1]["valueInteger"]}
    ...     25
    FOR     ${INDEX}    IN RANGE    0   ${response["expansion"]["parameter"][1]["valueInteger"]}-1
        ${code}     Set Variable    ${response["expansion"]["contains"][${INDEX}]["code"]}
        ${display}  Set Variable    ${response["expansion"]["contains"][${INDEX}]["display"]}
        Log To Console     Code=${code} , Display=${display}
    END
    [Teardown]      Reset Mock Server

Value Set GET Using Expand Operation - Positive - Count Is Zero
    [Documentation]     Get ValueSet using expand operation,
    ...     with valid:
    ...     - url
    ...     - count=0
    ...     Positive flow.
    [Tags]      Mock    VALUESET      Positive      Expand
    GET Create Mock Expectation - ValueSet - Expand Operation
    ...     ${MOCK_VALUE_SET_PATH}/expand_count_zero.json
    ...     None    200
    Send GET - ValueSet
    ...     endpoint=${VALUE_SET_EXPAND_ENDPOINT}   statusCode=200
    Should Be Equal As Strings      ${response["expansion"]["parameter"][1]["valueInteger"]}
    ...     0
    [Teardown]      Reset Mock Server

Value Set GET Using Expand Operation - Empty URL
    [Documentation]     Get ValueSet using expand operation,
    ...     with empty:
    ...     - url value.
    ...     Negative flow.
    [Tags]      Mock    VALUESET      Negative      Expand
    GET Create Mock Expectation - ValueSet - Expand Operation
    ...     ${MOCK_VALUE_SET_PATH}/expand_empty_url.json
    ...     emptyUrl    400
    Send GET - ValueSet
    ...     endpoint=${VALUE_SET_EXPAND_ENDPOINT}   invalidParameter=emptyUrl
    ...     statusCode=400
    Should Be Equal As Strings      ${response["issue"][0]["severity"]}
    ...     error
    Should Be Equal As Strings      ${response["issue"][0]["code"]}
    ...     invalid
    Should Contain      ${response["issue"][0]["diagnostics"]}
    ...     ValueSet url cannot be empty.
    [Teardown]      Reset Mock Server

Value Set GET Using Expand Operation - Wrong URL
    [Documentation]     Get ValueSet using expand operation,
    ...     with wrong:
    ...     - url value.
    ...     Negative flow.
    [Tags]      Mock    VALUESET      Negative      Expand
    GET Create Mock Expectation - ValueSet - Expand Operation
    ...     ${MOCK_VALUE_SET_PATH}/expand_wrong_url.json
    ...     wrongUrl    404
    Send GET - ValueSet
    ...     endpoint=${VALUE_SET_EXPAND_ENDPOINT}   invalidParameter=wrongUrl
    ...     statusCode=404
    Should Be Equal As Strings      ${response["issue"][0]["severity"]}
    ...     error
    Should Be Equal As Strings      ${response["issue"][0]["code"]}
    ...     not-found
    Should Contain      ${response["issue"][0]["diagnostics"]}
    ...     Could not find value set http://testurl.org.
    ...     If this is an implicit value set please make sure the url is correct.
    ...     Implicit values sets for different code systems are specified in
    ...     https://www.hl7.org/fhir/terminologies-systems.html .
    [Teardown]      Reset Mock Server

Value Set GET Using Expand Operation - Count With Minus
    [Documentation]     Get ValueSet using expand operation,
    ...     with count value containing minus sign.
    ...     Negative flow.
    [Tags]      Mock    VALUESET      Negative      Expand
    GET Create Mock Expectation - ValueSet - Expand Operation
    ...     ${MOCK_VALUE_SET_PATH}/expand_count_with_minus_sign.json
    ...     countMinus    422
    Send GET - ValueSet
    ...     endpoint=${VALUE_SET_EXPAND_ENDPOINT}   invalidParameter=countMinus
    ...     statusCode=422
    Should Be Equal As Strings     ${response["issue"][0]["severity"]}
    ...     error
    Should Be Equal As Strings     ${response["issue"][0]["code"]}
    ...     processing
    Should Be Equal As Strings     ${response["issue"][0]["diagnostics"]}
    ...     Invalid value for count

*** Keywords ***
Create Sessions
    Create Session          server      ${MOCK_URL}
    Create Mock Session     ${MOCK_URL}

Reset Mock Server
    Dump To Log
    Reset All Requests
