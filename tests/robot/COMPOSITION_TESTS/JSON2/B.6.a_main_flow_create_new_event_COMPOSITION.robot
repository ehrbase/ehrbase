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



Force Tags    JSON



*** Test Cases ***
Main flow create new event COMPOSITION

    upload OPT    nested/nested.opt

    create EHR

    commit composition (JSON)    nested/nested.composition.extdatetimes.xml

    # Check result data
                      Set Test Variable  ${template_id}    ${response.json()['archetype_details']['template_id']['value']}
                      Should Be Equal    ${template_id}    nested.en.v1
                      Set Test Variable  ${composer}       ${response.json()['composer']['name']}
                      Should Be Equal    ${composer}       Dr. House
                      Set Test Variable  ${setting}        ${response.json()['context']['setting']['value']}

        TRACE JIRA BUG  EHR-445  not-ready

                      Should Be Equal    ${setting}        primary nursing care

    [Teardown]    restart SUT
