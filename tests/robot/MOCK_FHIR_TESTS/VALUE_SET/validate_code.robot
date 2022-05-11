*** Settings ***
Documentation   ValueSet validate-code operation coverage by Robot Test Suite, using Mockup Server
...
Metadata        TOP_TEST_SUITE    MOCK_FHIR_TESTS

Resource        ../../_resources/keywords/mock_fhir_value_set_keywords.robot
Suite Setup     Create Sessions
Suite Teardown  Reset Mock Server
Force Tags      Mock    VALUESET    ValidateCode


*** Variables ***
${MOCK_VALUE_SET_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/mocks/value_set


*** Test Cases ***
Value Set GET Using Validate Code Operation - Valid Code And System URL
    [Documentation]     Get ValueSet using validate-code operation,
    ...     with valid code and system URL. Positive flow.
    [Tags]  Positive
    GET Create Mock Expectation - ValueSet - Validate Code operation
    ...     ${MOCK_VALUE_SET_PATH}/validate_code_success.json
    ...     invalidParameter=None   statusCode=200
    Send GET - ValueSet - Validate Code
    ...     endpoint=${VALUE_SET_VALIDATE_CODE_ENDPOINT}   statusCode=200
    [Teardown]      Reset Mock Server

Value Set GET Using Id
    [Documentation]     Get ValueSet using Id for ValueSet object.
    ...     Positive flow.
    [Tags]  Positive
    GET Create Mock Expectation - ValueSet - By Id
    ...     ${MOCK_VALUE_SET_PATH}/value_set_existing_id.json
    ...     existingId=true         statusCode=200
    Send GET - ValueSet - By Id
    ...     endpoint=${VALUE_SET_ENDPOINT}      existingId=true     statusCode=200
    Should Be Equal As Strings      ${response["resourceType"]}     ValueSet
    Should Be Equal As Strings      ${response["name"]}             idiosyncratic-adverse-reaction-type
    Should Be Equal As Strings      ${response["version"]}          1.0.0
    [Teardown]      Reset Mock Server

Value Set GET Using Not Existing Id
    [Documentation]     Get ValueSet using Id for ValueSet object.
    ...     ValueSet Id does not exist.
    ...     Negative flow.
    [Tags]  Negative
    GET Create Mock Expectation - ValueSet - By Id
    ...     ${MOCK_VALUE_SET_PATH}/value_set_not_existing_id.json
    ...     existingId=false        statusCode=404
    Send GET - ValueSet - By Id
    ...     existingId=false        statusCode=404
    Should Be Equal As Strings      ${response["issue"][0]["severity"]}         error
    Should Be Equal As Strings      ${response["issue"][0]["code"]}             not-found
    Should Contain                  ${response["issue"][0]["diagnostics"]}
    ...     ValueSet not found: not-existing-value-set-id
    [Teardown]      Reset Mock Server

Value Set GET Using Validate Code Operation - Valid Code - Invalid System URL
    [Documentation]     Get Value Set using validate-code operation,
    ...     with valid code, but invalid system URL. Negative flow.
    [Tags]  Negative
    GET Create Mock Expectation - ValueSet - Validate Code operation
    ...     ${MOCK_VALUE_SET_PATH}/value_set_system_url_not_found.json
    ...     invalidParameter=system   statusCode=200
    Send GET - ValueSet - Validate Code
    ...     endpoint=${VALUE_SET_VALIDATE_CODE_ENDPOINT}   statusCode=200
    Should Contain      ${response["parameter"][1]["valueString"]}
    ...     A version for code system http://notexistingsystemurl.org
    ...     was not supplied and the system could not find its latest version.
    ...     It does not appear to be indexed.
    [Teardown]      Reset Mock Server

Value Set GET Using Validate Code Operation - Valid System URL - Invalid Code
    [Documentation]     Get Value Set using validate-code operation,
    ...     with valid system URL, but invalid code. Negative flow.
    [Tags]  Negative
    GET Create Mock Expectation - ValueSet - Validate Code operation
    ...     ${MOCK_VALUE_SET_PATH}/value_set_code_not_found.json
    ...     invalidParameter=code   statusCode=200
    Send GET - ValueSet - Validate Code
    ...     endpoint=${VALUE_SET_VALIDATE_CODE_ENDPOINT}   statusCode=200
    Should Contain      ${response["parameter"][1]["valueString"]}
    ...     The specified code '111101111' is not known to belong to the specified
    ...     code system 'http://snomed.info/sct'
    [Teardown]      Reset Mock Server


*** Keywords ***
Create Sessions
    Create Session          server      ${MOCK_URL}
    Create Mock Session     ${MOCK_URL}

Reset Mock Server
    Dump To Log
    Reset All Requests
