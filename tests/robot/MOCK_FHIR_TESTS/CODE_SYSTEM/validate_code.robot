*** Settings ***
Documentation   CodeSystem validate-code operation coverage by Robot Test Suite, using Mockup Server
...
Metadata        TOP_TEST_SUITE    MOCK_FHIR_TESTS

Resource        ../../_resources/keywords/mock_fhir_code_system_keywords.robot
Suite Setup     Create Sessions
Suite Teardown  Reset Mock Server


*** Variables ***
${MOCK_CODE_SYSTEM_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/mocks/code_system


*** Test Cases ***
CODE SYSTEM GET Using Validate Code Operation - Valid Code And System URL
    [Documentation]     Get CodeSystem using validate-code operation,
    ...     with valid code and system URL. Positive flow.
    [Tags]      Mock    CODESYSTEM      Positive      ValidateCode
    GET Create Mock Expectation - CodeSystem - Validate Code operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/validate_code_success.json
    ...     None    200
    Send GET Expect Success - CodeSystem
    ...     endpoint=${CODE_SYSTEM_VALIDATE_CODE_ENDPOINT}
    Should Be Equal     ${response["parameter"][0]["valueBoolean"]}    ${true}
    Should Contain      ${response["parameter"][1]["valueString"]}
    ...      Bicarbonate [Moles/volume] in Serum or Plasma
    [Teardown]      Reset Mock Server

CODE SYSTEM GET Using Validate Code Operation - Valid System URL - Invalid Code
    [Documentation]     Get CodeSystem using validate-code operation,
    ...     with valid system URL, but invalid code. Negative flow.
    [Tags]      Mock    CODESYSTEM      Negative      ValidateCode
    GET Create Mock Expectation - CodeSystem - Validate Code operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/validate_code_code_not_found.json
    ...     code   200
    Send GET Expect Failure - CodeSystem - Validate Code (Invalid Code)
    ...     endpoint=${CODE_SYSTEM_VALIDATE_CODE_ENDPOINT}
    Should Be Equal     ${response["parameter"][0]["valueBoolean"]}    ${false}
    Should Contain      ${response["parameter"][1]["valueString"]}
    ...     The specified code '1111-9' is not known to belong
    ...     to the specified code system 'http://loinc.org'
    [Teardown]      Reset Mock Server

CODE SYSTEM GET Using Validate Code Operation - Valid Code - Invalid System URL
    [Documentation]     Get CodeSystem using validate-code operation,
    ...     with valid code URL, but invalid system URL. Negative flow.
    [Tags]      Mock    CODESYSTEM      Negative      ValidateCode
    GET Create Mock Expectation - CodeSystem - Validate Code operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/validate_code_system_url_not_found.json
    ...     system   404
    Send GET Expect Failure - CodeSystem - Validate Code (Invalid System URL)
    ...     endpoint=${CODE_SYSTEM_VALIDATE_CODE_ENDPOINT}
    Should Be Equal As Strings      ${response["issue"][0]["severity"]}     error
    Should Be Equal As Strings      ${response["issue"][0]["code"]}         not-found
    Should Contain      ${response["issue"][0]["diagnostics"]}
    ...     Code system
    ...     http://notexistingsystemurl.org could not be
    ...     resolved. It does not appear to be indexed.
    [Teardown]      Reset Mock Server

CODE SYSTEM GET Using Validate Code Operation - Missing Code And System URL Params
    [Documentation]     Get CodeSystem using validate-code operation,
    ...     code and system url not provided. Negative flow.
    [Tags]      Mock    CODESYSTEM      Negative      ValidateCode
    GET Create Mock Expectation - CodeSystem - Validate Code operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/validate_code_missing_code_and_system_url.json
    ...     empty   400
    Send GET Expect Failure - CodeSystem - Validate Code (Missing Code And System URL)
    ...     endpoint=${CODE_SYSTEM_VALIDATE_CODE_ENDPOINT}
    Should Be Equal As Strings     ${response["issue"][0]["severity"]}
    ...     error
    Should Be Equal As Strings     ${response["issue"][0]["code"]}
    ...     invalid
    Should Contain     ${response["issue"][0]["diagnostics"]}
    ...     A code/system pair must be provided.
    [Teardown]      Reset Mock Server


*** Keywords ***
Create Sessions
    Create Session          server      ${MOCK_URL}
    Create Mock Session     ${MOCK_URL}

Reset Mock Server
    Dump To Log
    Reset All Requests
