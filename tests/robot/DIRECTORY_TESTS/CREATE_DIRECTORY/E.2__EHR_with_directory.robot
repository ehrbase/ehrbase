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
Documentation    Alternative flow 1: create directory on EHR with directory
...
...     Preconditions:
...         An EHR with ehd_id exists, and has directory.
...
...     Flow:
...         1. Invoke the create directory service for the ehr_id
...            w/ same directory name that already exists.
...         2. The service should return an error, 
...            related to already existing EHR directory
...
...     Postconditions:
...         None
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 1: create directory on EHR with directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json
    validate POST response - 201 created directory

    create the same DIRECTORY again (JSON)    subfolders_in_directory.json
    validate POST response - 409 folder already exists
