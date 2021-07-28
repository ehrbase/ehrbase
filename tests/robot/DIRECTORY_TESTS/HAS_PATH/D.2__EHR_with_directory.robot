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
Documentation    Alternative flow 1: has path on EHR with just root directory
...
...     Preconditions:
...         An EHR with known ehr_id exists and has an empty directory (no subfolders or items).
...
...     Flow:
...         1. Invoke the has path service for the ehr_id and the path $path from the data set
...         2. The result must be $result from the data set
...
...     Postconditions:
...         None
...
...     Data set
...         DS   | $path                   | $result |
...         -----+-------------------------+---------+
...         DS 1 | /                       | true    |
...         DS 2 | _any_other_random_path_ | false   |
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

Suite Setup    Establish Preconditions
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 1: has path on EHR with just root directory (DS 1)

    get FOLDER in DIRECTORY at version (JSON)    /
    validate GET-@version response - 200 retrieved    root



Alternative flow 1: has path on EHR with just root directory (DS 2)

    generate random path
    get FOLDER in DIRECTORY at version (JSON)    ${path}
    validate GET-@version response - 404 unknown path




*** Keywords ***
Establish Preconditions
    create EHR
    create DIRECTORY (JSON)    empty_directory.json
