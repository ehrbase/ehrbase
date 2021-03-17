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
Metadata    Version    0.1.0
Metadata    Authors    *Jake Smolka*, *Wladislaw Wagner*
Metadata    Created    2021.01.26

Metadata        TOP_TEST_SUITE    EHR_STATUS
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags      COMPOSITION_get_versioned



*** Test Cases ***
1. Get Versioned Composition Of Existing EHR by Version UID (JSON)
    [Documentation]    Simple, valid request

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid}    ${response.body.uid.value}


1b. Get Versioned Composition Of Existing EHR With 2 Versions by Version UID (JSON)
    [Documentation]    Simple, valid request

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    Set Test Variable  ${version_uid}  ${version_uid[0:-1]}2

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid}    ${response.body.uid.value}


1c. Get Versioned Composition Of Existing EHR With 2 Versions by Invalid Version UID (JSON)
    [Documentation]    Simple, valid EHR_ID but invalid (non-existent) version

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests	

    update a composition for versioned composition tests

    Set Test Variable  ${version_uid}  ${version_uid[0:-1]}3

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    404


1d. Get Versioned Composition Of Existing EHR by Invalid Version UID (JSON)
    [Documentation]    Simple, invalid (negative) version number

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    Set Test Variable  ${version_uid}  ${version_uid[0:-1]}-2

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    400


1e. Get Versioned Composition Of Existing EHR by Invalid Version UID (JSON)
    [Documentation]    Simple, invalid (floating point) version number

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    Set Test Variable  ${version_uid}  ${version_uid[0:-1]}2.0

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    400


1f. Get Versioned Composition Of Existing EHR by Invalid Version UID (JSON)
    [Documentation]    Simple, invalid (random) version UID


    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests
    
    generate random version_uid
    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    400


2. Get Versioned Composition Of EHR by Version UID Invalid EHR (JSON)
    [Documentation]    Simple, invalid EHR_ID (non-existent)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests
    generate random ehr_id

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    404


3. Get Versioned Composition Of EHR by Version UID Invalid Versioned Object UID (JSON)
    [Documentation]    Simple, invalid EHR_ID (non-existent)

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests
    # comment: save orginal version uid
    ${original_id} =  Set Variable  ${version_uid}
    create fake composition
    # reset only this var to original
    ${version_uid} =  Set Variable  ${original_id}

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    404


4. Get Versioned Composition Of Existing EHR by Version UID Invalid Version UID (JSON)
    [Documentation]    Simple, valid EHR_ID but invalid version_uid

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    # comment: alter version uid to invalid one
    Set Test Variable  ${version_uid}  ${version_uid[0:-1]}2

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    404


5. Get Versioned Composition Of Existing EHR With 2 Versions by Version UID Invalid Version UID (JSON)
    [Documentation]    Simple, valid EHR_ID but invalid version_uid

    prepare new request session    JSON    Prefer=return=representation

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    generate random version_uid

    # comment: alter version uid to invalid one
    Set Test Variable  ${version_uid}  ${version_uid[0:-1]}2

    get version of versioned composition of EHR by UID    ${versioned_object_uid}    ${version_uid}
    Should Be Equal As Strings    ${response.status}    400
