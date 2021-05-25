*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition


*** Test Cases ***
Alternative flow 6 create new event COMPOSITION EHR doesnt exist RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=nested.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=nested.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist FLAT
    [Tags]    future
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=nested.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new event COMPOSITION EHR doesnt exist STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=nested.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist FLAT
    [Tags]    future
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition    404

Alternative flow 6 create new persistent COMPOSITION EHR doesnt exist STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition    404


*** Keywords ***
Precondition
    upload OPT    nested/nested.opt
    upload OPT    minimal_persistent/persistent_minimal.opt
    create fake EHR