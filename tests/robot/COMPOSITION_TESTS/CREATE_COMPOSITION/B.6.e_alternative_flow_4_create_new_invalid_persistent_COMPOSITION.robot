*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Alternative flow 4 create new invalid persistent COMPOSITION RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=persistent_minimal.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

Alternative flow 4 create new invalid persistent COMPOSITION RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=persistent_minimal.en.v1__invalid_wrong_structure.xml
    check status_code of commit composition    400

Alternative flow 4 create new invalid persistent COMPOSITION FLAT
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

Alternative flow 4 create new invalid persistent COMPOSITION TDD
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__invalid_wrong_structure.xml
    check status_code of commit composition    400

Alternative flow 4 create new invalid persistent COMPOSITION STRUCTURED
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__invalid_wrong_structure.json
    check status_code of commit composition    400

*** Keywords ***
Precondition
    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR