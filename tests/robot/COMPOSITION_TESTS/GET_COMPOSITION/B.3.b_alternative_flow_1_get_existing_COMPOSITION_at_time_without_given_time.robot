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
Alternative flow 1 get existing COMPOSITION at time, without given time
    [Tags]     

    Upload OPT    minimal/minimal_observation.opt
    create EHR
    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.xml
    update composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.v2.xml
    check composition update succeeded

    # comment: Check COPMOSITION exists and has correct content / Get version at time 1, should exist and be COMPO 1
    # NOTE: below keyword equals to `get composition - verstion at time`  without time parameter
    get composition - latest version    JSON
    check content of compositions latest version (JSON)

    #[Teardown]    restart SUT
