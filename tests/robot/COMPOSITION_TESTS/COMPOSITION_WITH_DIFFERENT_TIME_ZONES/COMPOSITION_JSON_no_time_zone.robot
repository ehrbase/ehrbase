# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
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

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/aql_query_keywords.robot


Force Tags

*** Test Cases ***
COMPOSITION JSON with no time zone using Get
    [Tags]    dtz

    Upload OPT    minimal/minimal_observation.opt

    create EHR

    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes_no_time_zone.xml
    get versioned composition - version at time    ${time_1}
    check content of compositions version at time (JSON)    time_1    original value


    #[Teardown]    restart SUT


COMPOSITION JSON with no time zone using AQL
    [Tags]    dtz

    Upload OPT    minimal/minimal_observation.opt

    create EHR

    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes_no_time_zone.xml
    Replace Uid With Actual  ${VALID QUERY DATA SETS}/${TIME QUERY DATA SET}  ${composition_uid}  ${VALID QUERY DATA SETS}/actual_uid_replaced.json
    Replace Uid With Actual  ${QUERY RESULTS LOADED DB}/${No Time Zone Expected DATA SET}  ${composition_uid}  ${QUERY RESULTS LOADED DB}/expected_uid_replaced.json
    execute ad-hoc query and check result (loaded DB)   actual_uid_replaced.json  expected_uid_replaced.json
    Remove File  ${VALID QUERY DATA SETS}/actual_uid_replaced.json
    Remove File  ${QUERY RESULTS LOADED DB}/expected_uid_replaced.json



    #[Teardown]    restart SUT
