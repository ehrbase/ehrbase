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
...      Alternative flow 3: get CONTRIBUTION on an EHR with CONTRIBUTIONS, by wrong CONTRIBUTION uid
...
...      Preconditions:
...          An EHR with known ehr_id exists and has CONTRIBUTIONS.
...
...      Flow:
...          1. Invoke get CONTRIBUTION service by the existing ehr_id and a random CONTRIBUTION uid
...          2. The result should be negative, with a message similar to "the CONTRIBUTION uid doesn't exist for the EHR ehr_id"
...
...      Postconditions:
...          None
Metadata        TOP_TEST_SUITE    CONTRIBUTION
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 3: get CONTRIBUTION on an EHR with CONTRIBUTIONS, by wrong CONTRIBUTION uid

    upload OPT    minimal/minimal_evaluation.opt

    create EHR

    commit CONTRIBUTION (JSON)    minimal/minimal_evaluation.contribution.json

    retrieve CONTRIBUTION by fake contri_uid (JSON)

    check response: is negative indicating non-existent contribution_uid
