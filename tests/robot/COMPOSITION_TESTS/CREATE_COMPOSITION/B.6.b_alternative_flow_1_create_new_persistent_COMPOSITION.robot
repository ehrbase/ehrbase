# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School),
# Nataliya Flusman (Solit Clouds), Nikita Danilin (Solit Clouds)
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Alternative flow 1 create new persistent COMPOSITION RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=persistent_minimal.en.v1__full_without_links.json
    check the successful result of commit composition

Alternative flow 1 create new persistent COMPOSITION RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=persistent_minimal.en.v1__full_without_links.xml
    check the successful result of commit composition

Alternative flow 1 create new persistent COMPOSITION FLAT
    [Tags]    future
    commit composition   format=FLAT
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successful result of commit composition   persistent_minimal

Alternative flow 1 create new persistent COMPOSITION TDD
    [Tags]    future
    commit composition   format=TDD
    ...                  composition=persistent_minimal.en.v1__full.xml
    check the successful result of commit composition

Alternative flow 1 create new persistent COMPOSITION STRUCTURED
    [Tags]    future
    commit composition   format=STRUCTURED
    ...                  composition=persistent_minimal.en.v1__full.json
    check the successful result of commit composition   persistent_minimal

*** Keywords ***
Precondition
    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR