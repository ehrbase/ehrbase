*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Alternative flow 1 create new persistent COMPOSITION RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successfull result of commit compostion

Alternative flow 1 create new persistent COMPOSITION RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=persistent_minimal.en.v1__full.xml
    check the successfull result of commit compostion

Alternative flow 1 create new persistent COMPOSITION FLAT
    [Tags]    future
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successfull result of commit compostion   persistent_minimal

Alternative flow 1 create new persistent COMPOSITION TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__full.xml
    check the successfull result of commit compostion

Alternative flow 1 create new persistent COMPOSITION STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successfull result of commit compostion   persistent_minimal

*** Keywords ***
Precondition
    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR