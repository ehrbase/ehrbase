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
Alternative flow 2 get COMPOSITION at time, COMPOSITION doesnt exist
    [Tags]     

    Upload OPT    minimal/minimal_observation.opt

    create EHR    XML

    # Commit fake COMPOSITION
    generate random composition_uid
    capture point in time    1

    get composition - version at time (XML)    ${time_1}
    check composition does not exist (version at time)

    [Teardown]    restart SUT
