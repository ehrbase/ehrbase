*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT

Force Tags      125    future


*** Test Cases ***
Alternative flow 2 create persistent COMPOSITION for the same archetype twice RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition   201
    commit composition   format=RAW_JSON
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition   400

Alternative flow 2 create persistent COMPOSITION for the same archetype twice RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition   201
    commit composition   format=RAW_XML
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition   400

Alternative flow 2 create persistent COMPOSITION for the same archetype twice FLAT
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition   201
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition   400

Alternative flow 2 create persistent COMPOSITION for the same archetype twice TDD
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition    201
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__full.xml
    check status_code of commit composition    400    

Alternative flow 2 create persistent COMPOSITION for the same archetype twice STRUCTURED
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition    201
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__full.json
    check status_code of commit composition    400    

*** Keywords ***
Precondition
    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR