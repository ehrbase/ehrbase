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

Resource    ${CURDIR}${/}../../_resources/keywords/composition_keywords.robot

# Resource    ${CURDIR}${/}../_resources/suite_settings.robot
# Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
# Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot
# Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot



Force Tags    XML



*** Test Cases ***
Alternative flow 3 update an existing persistent COMPOSITION referencing different template

    # Upload multiple OPTs
    upload OPT    minimal_persistent/persistent_minimal.opt    XML
    upload OPT    minimal_persistent/persistent_minimal_2.opt    XML

    create EHR    XML

    commit composition (XML)    minimal_persistent/persistent_minimal.composition.extdatetime.xml
    check content of composition (XML)

    # Commit a new version for the COMPOSITION (references the _2 OPT different than the referenced by the first committed COMPO)
    # Tries to commit as a new version of the first committed COMPO
    update composition (XML)    minimal_persistent/persistent_minimal.composition.extdatetime.v2_2.xml

        TRACE JIRA BUG    EHR-517    not-ready

    Should Be Equal As Strings   ${response.status_code}   400

    [Teardown]    restart SUT
