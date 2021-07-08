# Copyright (c) 2021 Jake Smolka (Hannover Medical School), 
#                    Wladislaw Wagner (Vitasystems GmbH), 
#                    Pablo Pazos (Hannover Medical School).
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
Metadata    Authors    *Jake Smolka*, *Wladislaw Wagner* 
Metadata    Created    2021.01.26

Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Test Setup
Test Teardown  

Force Tags      COMPOSITION_get_versioned



*** Test Cases ***
1. Get Versioned Composition Of Existing EHR (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    get versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${versioned_object_uid}    ${response.body.uid.value}
    Should Be Equal As Strings    ${ehr_id}    ${response.body.owner_id.id.value}


2. Get Versioned Composition Of Existing EHR With Two Status Versions (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    get versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${versioned_object_uid}    ${response.body.uid.value}
    Should Be Equal As Strings    ${ehr_id}    ${response.body.owner_id.id.value}


3. Get Versioned Composition Of Non-Existing EHR (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    create fake EHR

    get versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404


4. Get Versioned Composition Of Invalid EHR_ID (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    Set Test Variable    ${ehr_id}    foobar

    get versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404



5. Get Versioned Composition Of Non-Existing Composition (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    create fake composition

    get versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404


6. Get Versioned Composition Of Invalid Composition ID (JSON)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    Set Test Variable    ${versioned_object_uid}    foobar

    get versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404
