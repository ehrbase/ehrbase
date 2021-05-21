*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Alternative flow 5 create new event COMPOSITION referenced opt doesnt exist RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=nested.en.v1__invalid_opt_doesnt_exist.json
    check status_code of commit composition    422

Alternative flow 5 create new event COMPOSITION referenced opt doesnt exist RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=nested.en.v1__invalid_opt_doesnt_exist.xml
    check status_code of commit composition    422

Alternative flow 5 create new event COMPOSITION referenced opt doesnt exist TDD
    commit composition   format=TDD
    ...                  composition=nested.en.v1__invalid_opt_doesnt_exist.xml
    check status_code of commit composition    422

*** Keywords ***
Precondition
    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR