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
...      Alternative flow 9. commit CONTRIBUTION with COMPOSITION referencing a non existing OPT
...
...      Preconditions:
...          An EHR with known ehr_id exists, and there are no OPTs loaded.
...
...      Flow:
...          1. Invoke commit CONTRIBUTION service with an existing ehr_id and a valid VERSION<COMPOSITION>,
...             referencing a rando OPT template_id.
...          2. The result should be negative and retrieve an error related to the referenced OPT that should be loaded.
...
...      Postconditions:
...          None


Resource    ${CURDIR}${/}../../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/contribution_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/composition_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/template_opt1.4_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/ehr_keywords.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags    refactor



*** Test Cases ***
Alternative flow 9. commit CONTRIBUTION with COMPOSITION referencing a non existing OPT

      create EHR

            TRACE GITHUB ISSUE  51  not-ready  message=Next step fails due to a bug.

      commit invalid CONTRIBUTION (JSON)    ref_to_non_existent_OPT.json

      check response: is negative indicating non-existent OPT
