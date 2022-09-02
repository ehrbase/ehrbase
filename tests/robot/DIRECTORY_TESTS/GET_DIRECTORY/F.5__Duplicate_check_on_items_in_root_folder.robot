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
Documentation    Get folder after update and validate that one item is present in root folder
...
...     Based on reported bug: https://jira.vitagroup.ag/browse/CDR-523 - Fixed
...     Preconditions:
...         An EHR with ehr_id exists.
...
...     Flow:
...         1. Create Folder with one item in root
...         2. Get created Folder
...         3. Update Folder using the same content body as the create operation
...         4. Get updated Folder
...         5. The service should return only one item in the root Folder, the same as the initial one
...
...     Postconditions:
...         None
Metadata        TOP_TEST_SUITE    DIRECTORY

Resource        ../../_resources/keywords/directory_keywords.robot
Resource        ../../_resources/keywords/composition_keywords.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags


*** Test Cases ***
Get Folder After Update And Validate That One Item Is Present In Root Folder
    [Setup]     create EHR
    create DIRECTORY (JSON)    folder_with_one_item_in_root.json
    get DIRECTORY at version (JSON)
    ${nrOfItemsInRootFolder}    Get Length      ${response.json()["items"]}
    Should Be True      ${nrOfItemsInRootFolder} == 1   Folder contains more than 1 item in root
    ${firstDict}    Set Variable        ${response.json()["items"][0]}
    ${response}     Set Variable        ${EMPTY}
    update DIRECTORY (JSON)    folder_with_one_item_in_root.json
    validate PUT response - 200 updated
    get DIRECTORY at version (JSON)
    ${nrOfItemsInRootFolder}    Get Length      ${response.json()["items"]}
    Should Be True      ${nrOfItemsInRootFolder} == 1   Folder contains more than 1 item in root
    ${secondDict}   Set Variable       ${response.json()["items"][0]}
    Should Be Equal As Strings      ${firstDict}    ${secondDict}