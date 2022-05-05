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
Alternative flow 3 get COMPOSITION at version cover different versions

    Upload OPT    minimal_persistent/persistent_minimal.opt

    create EHR    XML

    commit composition (XML)    minimal_persistent/persistent_minimal.composition.extdatetime.xml

    update composition (XML)    minimal_persistent/persistent_minimal.composition.extdatetime.v2.xml
    check composition update succeeded

    # comment: Check COMPO v1 exist and has correct content
    get composition by composition_uid    ${version_uid_v1}
    check content of composition (XML)

    # comment: Check COMPO v2 exist and has correct content
    get composition by composition_uid    ${version_uid_v2}
    check content of updated composition (XML)

    # [Teardown]    restart SUT
