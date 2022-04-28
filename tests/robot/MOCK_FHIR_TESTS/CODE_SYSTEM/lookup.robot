*** Settings ***
Documentation   CodeSystem lookup operation coverage by Robot Test Suite
...
Metadata        TOP_TEST_SUITE    MOCK_FHIR_TESTS

Resource        ../../_resources/keywords/mock_fhir_code_system_keywords.robot
Suite Setup     Create Sessions
Suite Teardown  Reset Mock Server


*** Variables ***
${MOCK_CODE_SYSTEM_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/mocks/code_system


*** Test Cases ***
CODE SYSTEM GET Using Lookup Operation - Valid Code and System URL
    [Tags]      Mock    Positive    CODESYSTEM      Lookup
    GET Create Mock Expectation - CodeSystem - Lookup operation
    ...     ${MOCK_CODE_SYSTEM_PATH}/lookup_success.json
    Send GET Expect Success - CodeSystem - Lookup


*** Keywords ***
Create Sessions
    Create Session          server      ${MOCK_URL}
    Create Mock Session     ${MOCK_URL}

Reset Mock Server
    Dump To Log
    Reset All Requests