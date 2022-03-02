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
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/EHR_COMPOSITION.md#b6a-main-flow-create-new-event-composition
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/aql_query_keywords.robot

Suite Setup       Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Main flow Sanity Tests for FLAT Compositions
    [Tags]
    Get Web Template By Template Id  ${template_id}
    commit composition   format=FLAT
    ...                  composition=family_history__.json
    check the successful result of commit composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    Get Web Template By Template Id  ${template_id}
    (FLAT) get composition by composition_uid    ${composition_uid}
    Update Composition (FLAT)  family_history.v2__.json
    (FLAT) get composition by composition_uid    ${composition_uid}
    check composition exists

    [Teardown]    restart SUT


Main flow Sanity Tests for Canonical JSON Compositions
    [Tags]  wip
    Get Web Template By Template Id  ${template_id}
    commit composition   format=CANONICAL_JSON
    ...                  composition=nested.en.v1__full_without_links.json
    check the successful result of commit composition
    get composition by composition_uid    ${composition_uid}
    check composition exists

    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.xml
    check content of composition (JSON)

    update composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.v2.xml
    check content of updated composition (JSON)

    get composition by composition_uid    ${version_uid}
    check composition exists

    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes_no_time_zone.xml
    Replace Uid With Actual  ${VALID QUERY DATA SETS}/${TIME QUERY DATA SET}  ${composition_uid}  ${VALID QUERY DATA SETS}/actual_uid_replaced.json
    Replace Uid With Actual  ${QUERY RESULTS LOADED DB}/${No Time Zone Expected DATA SET}  ${composition_uid}  ${QUERY RESULTS LOADED DB}/expected_uid_replaced.json
    execute ad-hoc query and check result (loaded DB)   actual_uid_replaced.json  expected_uid_replaced.json
    Remove File  ${VALID QUERY DATA SETS}/actual_uid_replaced.json
    Remove File  ${QUERY RESULTS LOADED DB}/expected_uid_replaced.json

    [Teardown]    restart SUT

Main flow Sanity Tests for Canonical XML Compositions
    [Tags]
    Get Web Template By Template Id  ${template_id}
    commit composition   format=CANONICAL_XML
    ...                  composition=nested.en.v1__full_without_links.xml
    check the successful result of commit composition
    get composition by composition_uid    ${composition_uid}
    check composition exists

    commit composition (XML)    minimal/minimal_observation.composition.participations.extdatetimes.xml
    check content of composition (XML)

    update composition (XML)    minimal/minimal_observation.composition.participations.extdatetimes.v2.xml
    check content of updated composition (XML)

    get composition by composition_uid    ${version_uid}
    check composition exists

    [Teardown]    restart SUT



*** Keywords ***
Precondition
    Upload OPT    all_types/family_history.opt
    Upload OPT    nested/nested.opt
    upload OPT    minimal/minimal_observation.opt
    Extract Template_id From OPT File
    create EHR