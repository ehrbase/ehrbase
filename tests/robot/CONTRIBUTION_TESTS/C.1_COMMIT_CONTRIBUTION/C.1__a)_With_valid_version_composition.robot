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
...     Main flow: successfully commit CONTRIBUTION with single valid VERSION<COMPOSITION>
...
...     Preconditions:
...         An EHR with known ehr_id exists, and OPTs should be loaded for each valid case.
...
...     Flow:
...         1. Invoke commit CONTRIBUTION service with an existing ehr_id and valid data sets,
...            that reference existing OPTs in the system.
...         2. The result should be positive and retrieve the id of the CONTRIBUTION just created
...         3. Verify the existing CONTRIBUTION uids and the amount of existing CONTRIBUTIONS
...            for the EHR
...
...     Postconditions:
...         The EHR with ehr_id will have a new CONTRIBUTION.


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
Main flow: successfully commit CONTRIBUTION with single valid VERSION<COMPOSITION>

    upload OPT    minimal/minimal_evaluation.opt
    create EHR
    commit CONTRIBUTION (JSON)  minimal/minimal_evaluation.contribution.json
    check response: is positive - returns version id
    check content of committed CONTRIBUTION

    [Teardown]    restart SUT
