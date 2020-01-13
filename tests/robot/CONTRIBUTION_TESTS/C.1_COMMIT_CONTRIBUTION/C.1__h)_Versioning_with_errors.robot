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
...     Alternative flow 7: commit CONTRIBUTIONS for versioning, but second commit contains errors
...
...     Preconditions:
...         An EHR with known ehr_id exists, and OPTs should be loaded for each case.
...
...     Flow:
...         1. Invoke commit CONTRIBUTION service with an existing ehr_id and a valid VERSION<COMPOSITION>, that reference existing OPTs in the system.
...         2. The result should be positive, returning the version id for the created VERSION
...         3.  Invoke commit CONTRIBUTION service with an existing ehr_id and a valid VERSION<COMPOSITION> that should have the same template_id as the one used in 1., change_type = modification and preceding_version_uid = version id returned in 2. but invalid data in the COMPOSITION, and should reference existing OPTs in the system.
...         4. The result should be negative, and retrieve some info about the errors found on the data committed.
...
...     Postconditions:
...         There will be just one VERSION in the EHR with ehr_id.


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
Alternative flow 7: commit CONTRIBUTIONS for versioning, but second commit contains errors

    upload OPT    minimal/minimal_admin.opt

    create EHR

    commit CONTRIBUTION (JSON)    minimal/minimal_admin.contribution.json

    check response: is positive - returns version id

    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/minimal_admin.contribution.modification.incomplete.json

        TRACE GITHUB ISSUE  71  not-ready

    check response: is negative indicating errors in committed data

    [Teardown]    restart SUT
