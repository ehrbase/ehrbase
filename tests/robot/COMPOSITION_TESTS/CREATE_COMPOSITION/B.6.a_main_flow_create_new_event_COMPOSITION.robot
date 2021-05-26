*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup       Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Main flow create new event COMPOSITION RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=nested.en.v1__full_without_links.json
    check the successful result of commit composition

Main flow create new event COMPOSITION RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=nested.en.v1__full_without_links.xml
    check the successful result of commit composition

Main flow create new event COMPOSITION FLAT
    [Tags]    future
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.json
    check the successful result of commit composition   nesting

Main flow create new event COMPOSITION TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=nested.en.v1__full.xml
    check the successful result of commit composition

Main flow create new event COMPOSITION STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=nested.en.v1__full.json
    check the successful result of commit composition   nesting

*** Keywords ***
Precondition
    upload OPT    nested/nested.opt
    create EHR