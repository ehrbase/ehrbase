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

Force Tags



*** Test Cases ***
Alternative flow update existing event COMPOSITION when EHR status is_modifiable False

    Upload OPT    minimal/minimal_observation.opt

    create EHR

    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.xml
    check content of composition (JSON)
    update EHR: set ehr-status modifiable    ${FALSE}

    update composition - is modifiable false (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.v2.xml
    Status Should Be        409
    ${finalString}       Decode Bytes To String      ${response}     UTF-8
    Should Contain      ${finalString}        does not allow modification
    [Teardown]    restart SUT
