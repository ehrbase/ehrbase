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


Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

# Setup/Teardown from __init.robot is used
# Suite Setup         startup SUT
# Suite Teardown      shutdown SUT

Force Tags    ehr_status



*** Test Cases ***
Get Status Of Existing EHR (JSON)

    prepare new request session    JSON

    create new EHR

    get ehr_status of EHR

    Verify Response (JSON)


Get Status Of Existing EHR (XML)

    prepare new request session    XML

    create new EHR (XML)

    get ehr_status of EHR

    verify response (XML)





*** Keywords ***
verify response (JSON)
                        Integer     response status         200

                        String    response body uid value    ${ehrstatus_uid}::local.ehrbase.org::1
                        String    response body subject external_ref id value    ${subject_Id}
                        
        TRACE GITHUB ISSUE  NO-ID-YET  not-ready

                        Object    response body    ${ehr_status}


verify response (XML)
                        Should Be Equal As Strings   ${response.status}  200
                        # has or not a subject_id
                        # has correct value for is_modifiable
                        # has correct value for is_queryable
