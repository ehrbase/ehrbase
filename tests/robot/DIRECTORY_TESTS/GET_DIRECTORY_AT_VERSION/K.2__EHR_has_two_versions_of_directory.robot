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
Documentation    Alternative flow 1: get directory at version from existent EHR that has two versions of directory
...
...     Preconditions:
...         An EHR with known ehr_id exists in the server, has two versions of directory.
...
...     Flow:
...         1. Invoke the GET DIRECTORY AT VERSION service for the ehr_id and
...            the version_uid of the FIRST version of directory
...         2. The service should return the first version of the directory
...         3. Invoke the GET DIRECTORY AT VERSION service for the ehr_id and
...            the version_uid of the SECOND version of directory
...         4. The service should return the second version of the directory
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

Force Tags    refactor



*** Test Cases ***
Alternative flow 1: get directory at version from existent EHR that has two versions of directory

    create EHR
    create DIRECTORY (JSON)    subfolders_in_directory.json
    validate POST response - 201 created directory

    get DIRECTORY at version (JSON)
    validate GET-@version response - 200 retrieved    root
    

    update DIRECTORY (JSON)    subfolders_in_directory_with_details.json
    validate PUT response - 200 updated

    get DIRECTORY at version (JSON)
    # TODO: check that it is the SECOND version
    validate GET-@version response - 200 retrieved    root
