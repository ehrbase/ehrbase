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
Metadata    Version    0.1.0
Metadata    Author    *Wladislaw Wagner*
Metadata    Created    2019.03.03

Documentation   EHR Special Cases
...

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

Suite Setup    startup SUT
Suite Teardown    shutdown SUT

Force Tags  create_ehr    obsolete



*** Test Cases ***
# SPECIAL TEST CASES NOT COVERED BY TEST DATA SETS

POST /ehr
    [Documentation]  post to /ehr endpoint
    ...              without any query parameters
    ...              without any path params
    ...              without body/payload

    &{R}=    REST.POST    /ehr
    Set Test Variable    ${response}    ${R}
    Integer   response status    201
    [Teardown]  TE @Dev subjectId and subjectNamespace must be optional - tag(s): not-ready

POST /ehr?subjectId=
    [Documentation]  subjectId provided but without value

    &{R}=    REST.POST    /ehr?subjectId=
    Set Test Variable    ${response}    ${R}
    Integer   response status    400
    [Teardown]  TE @Dev Response status should be 400 Bad Request - tag(s): not-ready

POST /ehr?subjectId=&subjectNamespace=
    [Documentation]  subjectId and subjectNamespace provided without value

    &{R}=    REST.POST    /ehr?subjectId=&subjectNamespace=
    Set Test Variable    ${response}    ${R}
    Integer   response status    400

POST /ehr?subjectId=given222&subjectNamespace=
    [Documentation]  subjectId  OK but subjectNamespace without value
    ...               If subjectId provided then subjectNamespace is mandatory

    &{R}=    REST.POST    /ehr?subjectId=222&subjectNamespace=
    Set Test Variable    ${response}    ${R}
    Integer   response status    400
    [Teardown]  TE @Dev Response status should be 400 Bad Request - tag(s): not-ready

POST /ehr?subjectId=&subjectNamespace=given333
    [Documentation]  subjectNamespace OK, but subjectId without value

    &{R}=    REST.POST    /ehr?subjectId=&subjectNamespace=333
    Set Test Variable    ${response}    ${R}
    Integer   response status    400



*** Keywords ***
