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

Documentation   General setting for OpenEHR test suites.
# Metadata    Version        1.0

Library    REST    ${baseurl}    #ssl_verify=false
Library    RequestsLibrary  WITH NAME  R
Library    String
Library    Collections
Library    OperatingSystem
Library    Process
Library    XML
Library    JSONLibrary
Library    DateTime
Library    ${CURDIR}${/}../_resources/libraries/dockerlib.py
Library    ${CURDIR}${/}../_resources/libraries/jsonlib.py
Resource   ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
Resource   ${CURDIR}${/}../_resources/keywords/db_keywords.robot




*** Variables ***
${hip_baseurl_v1}     http://localhost:8080/ehrbase/rest/ecis/v1
${template_id}    IDCR%20-%20Immunisation%20summary.v0
${invalid_ehr_id}    123
${CODE_COVERAGE}    False

${PROJECT_ROOT}  ${EXECDIR}${/}..
${POM_FILE}    ${PROJECT_ROOT}${/}pom.xml
${FIXTURES}     ${PROJECT_ROOT}/tests/robot/_resources/fixtures


${SUT}                 EHRBASE

&{EHRSCAPE}            URL=https://rest.ehrscape.com/rest/openehr/v1
...                    CREDENTIALS=@{scapecreds}
...                    AUTH={"Authorization": "Basic YmlyZ2VyLmhhYXJicmFuZHRAcGxyaS5kZTplaHI0YmlyZ2VyLmhhYXJicmFuZHQ"}
@{scapecreds}          birger.haarbrandt@plri.de    ehr4birger.haarbrandt

&{EHRBASE}             URL=http://localhost:8080/ehrbase/rest/openehr/v1
...                    CREDENTIALS=${basecreds}
...                    AUTH={ "Authorization": null}
${basecreds}           None

${baseurl}             ${${SUT}.URL}
${authorization}        ${${SUT}.AUTH}



*** Keywords ***
