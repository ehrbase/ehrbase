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
Alternative flow 3 get COMPOSITION at version cover different versions

    upload OPT    minimal_persistent/persistent_minimal.opt    XML

    create EHR    XML

    commit composition (XML)    minimal_persistent/persistent_minimal.composition.extdatetime.xml

    update composition (XML)    minimal_persistent/persistent_minimal.composition.extdatetime.v2.xml
    check composition update succeeded

    # Check COMPO v1 exist and has correct content
    # composition_keywords.start request session    application/xml    application/xml

    get composition by composition_uid    ${version_uid_v1}

        TRACE JIRA BUG  NO-JIRA-ID  not-ready  unreported bug: <Map><error/><status>Internal Server Error</status></Map>

    check content of composition (XML)

    # Check COMPO v2 exist and has correct content
    get composition by composition_uid    ${version_uid_v2}
    check content of updated composition (XML)

    # [Teardown]    restart SUT
