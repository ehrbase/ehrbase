*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Alternative flow 3 create new invalid event COMPOSITION RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=nested.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=nested.en.v1__invalid_wrong_structure.xml
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION FLAT
    [Tags]    future
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=nested.en.v1__invalid_wrong_structure.xml
    check status_code of commit composition    400

Alternative flow 3 create new invalid event COMPOSITION STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=nested.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

*** Keywords ***
Precondition
    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR