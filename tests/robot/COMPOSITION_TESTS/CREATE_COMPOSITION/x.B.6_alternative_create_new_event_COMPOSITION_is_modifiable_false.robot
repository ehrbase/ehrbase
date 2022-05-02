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
...             Provide URL for documentation
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/generic_keywords.robot

Suite Setup       Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Main flow create new event COMPOSITION CANONICAL_JSON
    update EHR: set ehr-status modifiable    ${FALSE}
    commit composition   format=CANONICAL_JSON
    ...                  composition=nested.en.v1__full_without_links.json
    check response: is negative indicating does not allow modification

Main flow create new event COMPOSITION CANONICAL_XML
    commit composition   format=CANONICAL_XML
    ...                  composition=nested.en.v1__full_without_links.xml
    check response: is negative indicating does not allow modification
    ...                  responseType=XML

Main flow create new event COMPOSITION FLAT
    [Tags]
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.xml.flat.json
    check response: is negative indicating does not allow modification

*** Keywords ***
Precondition
    Upload OPT    nested/nested.opt
    create EHR