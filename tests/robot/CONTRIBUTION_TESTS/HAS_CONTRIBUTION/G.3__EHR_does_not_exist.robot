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
Documentation   Contribution Integration Tests
...
...     Alternative flow 2: has CONTRIBUTION, EHR doesn't exist
...
...     Preconditions:
...         None.
...
...     Flow:
...         1. Invoke has CONTRIBUTION service with a random ehr_id and a random CONTRIBUTION uid
...         2. The result should be negative "the EHR with ehd_id doesn't exist"

...     Postconditions:
...         None.
Metadata        TOP_TEST_SUITE    CONTRIBUTION
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags    refactor



*** Test Cases ***
Alternative flow 2 has CONTRIBUTION EHR doesnt exists

    retrieve CONTRIBUTION by fake ehr_id & contri_uid (JSON)

    check response: is negative indicating non-existent ehr_id
