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
Documentation    Alternative flow 1: delete directory from EHR with directory
...
...     Preconditions:
...         An EHR with ehr_id exists and has directory.
...
...     Flow:
...         1. Invoke the delete directory service for the ehr_id
...         2. The service should return a positive result related with the deleted directory
...
...     Postconditions:
...         The EHR ehr_id doesn't have directory
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 1: delete directory from EHR with directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory_items.json

    delete DIRECTORY (JSON)

    validate DELETE response - 204 deleted
