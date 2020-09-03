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
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

Force Tags      JSON



*** Test Cases ***
Alternative flow 2 create persistent COMPOSITION for the same archetype twice
    [Tags]        125    future

    upload OPT    minimal_persistent/persistent_minimal.opt
    create EHR
    commit composition (JSON)    minimal_persistent/persistent_minimal.composition.extdatetime.xml

    # comment: Another commit for the same persistent archetype/template to the same EHR
    commit same composition again    minimal_persistent/persistent_minimal.composition.extdatetime.xml

    [Teardown]    restart SUT
