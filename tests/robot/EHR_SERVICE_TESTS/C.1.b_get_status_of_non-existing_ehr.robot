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

Documentation   C.1.b) Alternative flow 1: Get status of a non existing EHR
...
...             Preconditions:
...                 The server should be empty (no EHRs, no commits, no OPTs).
...
...             Postconditions:
...                 None
...
...             Flow:
...                 1. Invoke the get EHR_STATUS service by a random ehr_id
...                 2. The result should be negative and the result should include an error
...                    related to "EHR with ehr_id doesn't exists".


Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

# Setup/Teardown from __init.robot is used
# Suite Setup       startup SUT
# Suite Teardown    shutdown SUT

Force Tags    ehr_status


*** Comments ***
EHRBASE:    endpoint not implemented yet, resulting in a false positive.




*** Test Cases ***
Get status of a non existing EHR

    prepare new request session    JSON

    create fake EHR

    get ehr_status of fake EHR
