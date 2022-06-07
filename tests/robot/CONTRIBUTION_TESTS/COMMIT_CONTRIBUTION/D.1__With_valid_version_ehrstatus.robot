# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Jake Smolka (Hannover Medical School).
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
...     Main flow: successfully commit CONTRIBUTION with single valid VERSION<EHR_STATUS>
...
...     Preconditions:
...         An EHR with known ehr_id exists.
...
...     Flow:
...         1. Invoke commit CONTRIBUTION service with an existing ehr_id and valid data sets.
...         2. The result should be positive and retrieve the id of the CONTRIBUTION just created
...         3. Verify the existing CONTRIBUTION uids and the amount of existing CONTRIBUTIONS
...            for the EHR
...
...     Postconditions:
...         The EHR with ehr_id will have a new CONTRIBUTION.

Metadata        TOP_TEST_SUITE    CONTRIBUTION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/keywords/contribution_keywords.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags    refactor



*** Test Cases ***
Main flow: successfully commit CONTRIBUTION with single valid VERSION<EHR_STATUS> to modify

    create EHR
    # comment: get version uid of status from create EHR and set it to variable which is access in KW below
    Set Test Variable  ${version_id}  ${ehrstatus_uid}
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/status.contribution.modification.json
    check response: is positive - contribution has new version

    #[Teardown]    restart SUT
