# Copyright (c) 2021 Jake Smolka (Hannover Medical School), 
#                    Wladislaw Wagner (Vitasystems GmbH), 
#                    Pablo Pazos (Hannover Medical School).
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
Metadata    Authors    *Jake Smolka*, *Wladislaw Wagner* 
Metadata    Created    2021.01.26

Metadata        TOP_TEST_SUITE    EHR_STATUS
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
1. Get Versioned Status Of Existing EHR (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    get versioned ehr_status of EHR
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}
    Should Be Equal As Strings    ${ehr_id}    ${response.body.owner_id.id.value}


2. Get Versioned Status Of Existing EHR With Two Status Versions (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    update EHR: set ehr_status is_queryable    ${TRUE}
    check response of 'update EHR' (JSON)

    get versioned ehr_status of EHR
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid[0:-1]}2    ${response.body.uid.value}
    Should Be Equal As Strings    ${ehr_id}    ${response.body.owner_id.id.value}


3. Get Versioned Status Of Non-Existing EHR (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create fake EHR

    get versioned ehr_status of EHR
    Should Be Equal As Strings    ${response.status}    404


4. Get Versioned Status Of Invalid EHR_ID (JSON)

    prepare new request session    JSON    Prefer=return=representation

    Set Test Variable    ${ehr_id}    foobar

    get versioned ehr_status of EHR
    Should Be Equal As Strings    ${response.status}    404
