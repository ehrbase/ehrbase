*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Main flow create new event COMPOSITION RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successfull result of commit compostion

Main flow create new event COMPOSITION RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=persistent_minimal.en.v1__full.xml
    check the successfull result of commit compostion

Main flow create new event COMPOSITION FLAT
    [Tags]    not-ready    bug
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successfull result of commit compostion   persistent_minimal

Main flow create new event COMPOSITION TDD
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__full.xml
    check the successfull result of commit compostion

Main flow create new event COMPOSITION STRUCTURED
    [Tags]    not-ready    bug
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successfull result of commit compostion   persistent_minimal

*** Keywords ***
Precondition
    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR