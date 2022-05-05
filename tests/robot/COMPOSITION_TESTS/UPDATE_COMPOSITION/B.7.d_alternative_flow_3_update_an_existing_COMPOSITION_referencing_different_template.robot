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
Alternative flow 3 update an existing persistent COMPOSITION referencing different template

    # comment: Upload multiple OPTs
    Upload OPT    minimal_persistent/persistent_minimal.opt
    Upload OPT    minimal_persistent/persistent_minimal_2.opt

    create EHR

    commit composition (JSON)    minimal_persistent/persistent_minimal.composition.extdatetime.xml
    check content of composition (JSON)

    # comment: Commit a new version for the COMPOSITION referencing _2 OPT which is a different one 
    #          than the one referenced by the first committed COMPO
    update composition - invalid opt reference (JSON)    minimal_persistent/persistent_minimal.composition.extdatetime.v2_2.xml
    Should Be Equal As Strings   ${response.status_code}   400

    [Teardown]    restart SUT
