*** Settings ***
Documentation   CodeSystem lookup operation coverage by Robot Test Suite, using Mockup Server
...
Metadata        TOP_TEST_SUITE    MOCK_FHIR_TESTS

Resource        ../../_resources/keywords/mock_fhir_code_system_keywords.robot
Suite Setup     Create Sessions
Suite Teardown  Reset Mock Server


*** Variables ***
${MOCK_CODE_SYSTEM_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/mocks/code_system


*** Test Cases ***
CODE SYSTEM GET Using Lookup Operation - Valid Code and System URL
    [Documentation]     Get CodeSystem using lookup operation, with valid code and system URL.
    [Tags]      Mock    CODESYSTEM      Positive      Lookup
    GET Create Mock Expectation - CodeSystem - Lookup operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/lookup_success.json
    ...     None    200
    Send GET Expect Success - CodeSystem - Lookup
    Should Contain      ${response["parameter"][0]["valueString"]}
    ...      Bicarbonate [Moles/volume] in Serum or Plasma
    [Teardown]      Reset Mock Server

CODE SYSTEM GET Using Lookup Operation - Valid System URL - Invalid Code
    [Documentation]     Get CodeSystem using lookup operation, with valid system URL, but invalid code.
    [Tags]      Mock    CODESYSTEM      Negative      Lookup
    GET Create Mock Expectation - CodeSystem - Lookup operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/lookup_code_not_found.json
    ...     code   404
    Send GET Expect Failure - CodeSystem - Lookup (Invalid Code)
    Should Contain      ${response["issue"][0]["diagnostics"]}
    ...     The code 1111-9 (http://loinc.org:2.72) was not found.
    [Teardown]      Reset Mock Server

CODE SYSTEM GET Using Lookup Operation - Valid Code - Invalid System URL
    [Documentation]     Get CodeSystem using lookup operation, with valid code URL, but invalid system URL.
    [Tags]      Mock    CODESYSTEM      Negative      Lookup
    GET Create Mock Expectation - CodeSystem - Lookup operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/lookup_system_url_not_found.json
    ...     system   404
    Send GET Expect Failure - CodeSystem - Lookup (Invalid System URL)
    Should Contain     ${response["issue"][0]["diagnostics"]}
    ...     Code system http://notexistingsystemurl.org could not be resolved. It does not appear to be indexed.
    [Teardown]      Reset Mock Server

CODE SYSTEM GET Using Lookup Operation - Missing Code and System URL Params
    [Documentation]     Get CodeSystem using lookup operation, code and system url not provided.
    [Tags]      Mock    CODESYSTEM      Negative      Lookup
    GET Create Mock Expectation - CodeSystem - Lookup operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/lookup_missing_code_and_system_url.json
    ...     empty   400
    Send GET Expect Failure - CodeSystem - Lookup (Missing Code And System URL)
    Should Contain     ${response["issue"][0]["diagnostics"]}
    ...     Either a code and system or a coding are required.
    [Teardown]      Reset Mock Server


*** Keywords ***
Create Sessions
    Create Session          server      ${MOCK_URL}
    Create Mock Session     ${MOCK_URL}

Reset Mock Server
    Dump To Log
    Reset All Requests
