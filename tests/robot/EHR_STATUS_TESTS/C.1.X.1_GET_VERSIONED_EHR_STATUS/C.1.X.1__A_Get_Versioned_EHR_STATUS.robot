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
Metadata    Created    2019.07.02

Documentation   C.1.a) Main flow: Get status of an existing EHR
...             Preconditions:
...                 An EHR with known ehr_id should exist.
...
...             Postconditions:
...                 None
...
...             Flow:
...                 1. Invoke the get EHR_STATUS service by the existing ehr_id
...                 2. The result should be positive and retrieve a correspondent EHR_STATUS.
...                    The EHR_STATUS internal information should match the rules in which
...                    the EHR was created (see test flow Create EHR) and those should be verified:
...
...                    a) has or not a subject_id
...                    b) has correct value for is_modifiable
...                    c) has correct value for is_queryable
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


# Get Status Of Existing EHR (XML)

#     prepare new request session    XML    Prefer=return=representation
#     create new EHR (XML)
#     get ehr_status of EHR
#     verify response (XML)


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


