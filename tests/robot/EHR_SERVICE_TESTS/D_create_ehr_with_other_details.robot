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
Metadata    Author    *Pablo Pazos*
Metadata    Created    2019.12.02

Documentation   D) Main flow: Create new EHR with other_details
...
...            https://github.com/ehrbase/project_management/issues/55
...

Resource    ${CURDIR}${/}../suite_settings.robot
Resource    generic_keywords.robot
Resource    ehr_keywords.robot

# Suite Setup    startup SUT
# Suite Teardown    shutdown SUT
# Test Template    client sends POST request to /ehr

Force Tags    create_ehr

*** Test Cases ***
Create Same EHR Twice For The Same Patient (JSON)

    start request session    JSON

    generate random subject_id
    create new EHR with other_details for subject_id (JSON)    ${subject_id}

    verify response


*** Keywords ***
verify response
    Should Be Equal As Strings    ${response.status}    201
    #${json_response}=  Set Variable  ${response.json()}
    ${json_ehr_expected}=        Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec_with_other_details.json
    ${json_ehr_expected}=        Update Value To Json  ${json_ehr}   $.subject.external_ref.id.value   ${subject_id}
    &{diff}=            compare jsons    ${response.body.json()}    ${json_ehr_expected}
    Should Be Empty  ${diff}  msg=DIFF DETECTED!
