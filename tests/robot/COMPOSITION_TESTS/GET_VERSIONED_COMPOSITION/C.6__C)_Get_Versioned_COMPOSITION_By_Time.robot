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

Metadata        TOP_TEST_SUITE    EHR_STATUS

Resource        ../../_resources/keywords/composition_keywords.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags      COMPOSITION_get_versioned



*** Test Cases ***
1. Get Composition via Versioned Composition Of Existing EHR by Time Without Query (JSON)
    [Documentation]    Simple test without query param

    create EHR and commit a composition for versioned composition tests

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid}    ${response.body.uid.value}


2. Get Composition via Versioned Composition Of Existing EHR by Time With Query (JSON)
    [Documentation]    Test with query param

    create EHR and commit a composition for versioned composition tests

    # comment: set the query parameter to current data and format as openEHR REST spec conformant timestamp
    ${date} = 	Get Current Date    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid}    ${response.body.uid.value}


3. Get Composition via Versioned Composition Of Existing EHR by Time With Query (JSON)
    [Documentation]    Test with and without query param and multiple versions

    create EHR and commit a composition for versioned composition tests
    # comment: save orginal version uid
    ${original_id} =  Set Variable  ${version_uid}

    capture point in time  after_compo_creation
    Log    ${time_after_compo_creation}

    update a composition for versioned composition tests

    # comment: 1. check if latest version gets returned without parameter
    Log    GET VERSIONED COMPOSITION (LATEST)
    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid[0:-1]}2    ${response.body.uid.value}

    # comment: 2. check if current time returns latest version too
    Log    GET VERSIONED COMPOSITION (LATEST - BY CURRENT TIME)
    # comment: set the query parameter to current date/time and format as openEHR REST spec conformant timestamp
    ${current_time} =    Get Current Date    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${current_time}     # set query as dictionary
    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid[0:-1]}2    ${response.body.uid.value}

    # comment: 3. check if original timestamp returns original version
    Log    GET VERSIONED COMPOSITION (ORIGINAL - BY CREATION TIME)
    Set Test Variable 	&{query} 	version_at_time=${time_after_compo_creation}     # set query as dictionary
    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${original_id}    ${response.body.uid.value}


4. Get Composition via Versioned Composition Of Existing EHR by Time Check Lifecycle State (JSON)
    [Documentation]    Simple, but checking lifecycle state

    create EHR and commit a composition for versioned composition tests

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid}    ${response.body.uid.value}
    Should Be Equal As Strings    complete   ${response.body.lifecycle_state.value}


5a. Get Composition via Versioned Composition Of Existing EHR by Time Check Preceding Version (JSON)
    [Documentation]    Simple, but checking preceding version uid - with one version

    create EHR and commit a composition for versioned composition tests
    
    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${version_uid}    ${response.body.uid.value}
    Should Not Contain  ${response.body}  preceding_version_uid


5b. Get Composition via Versioned Composition Of Existing EHR by Time Check Preceding Version (JSON)
    [Documentation]    Simple, but checking preceding version uid - with two versions

    create EHR and commit a composition for versioned composition tests
    # comment: save orginal version uid
    Set Test Variable  ${original_id}  ${version_uid}

    update a composition for versioned composition tests

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${original_id}    ${response.body.preceding_version_uid.value}


# TODO: figure out how to address the variable
6. Get Composition via Versioned Composition Of Existing EHR by Time Check Data (JSON)
    [Documentation]    Simple, but checking object data - with two versions
    [Tags]

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    # comment: the target string is (in basic style): response.body.data.content[0].data.events[0].data.items[0].value.value
    ${items} =    Set Variable    ${response.body.data.content[0].data.events[0].data["items"]}
    ${target_string} =   Set Variable    ${items[0].value.value}

    Should Be Equal As Strings    ${target_string}    modified value


7a. Get Composition via Versioned Composition Of Existing EHR by Time With Parameter Check (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    create EHR and commit a composition for versioned composition tests

    # comment: set the query parameter to current data in openEHR REST spec conformant timestamp
    ${date} = 	Get Current Date    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200


7b. Get Composition via Versioned Composition Of Existing EHR With Invalid Timestamp As Parameter (JSON)
    [Documentation]    Checking for expected responses with invalid parameters

    create EHR and commit a composition for versioned composition tests

    # comment: generate a timestamp which is considered invalid by openEHR REST Spec
    ${date} = 	Get Current Date
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    400


7c. Get Composition via Versioned Composition Of Non-Existent EHR by Time With Parameter Check (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    create EHR and commit a composition for versioned composition tests

    create fake EHR

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404


7d. Get Composition via Versioned Composition Of Non-Existent Composition by Time With Parameter Check (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    create EHR and commit a composition for versioned composition tests
    create fake composition

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404


7e. Get Composition via Versioned Composition Of Existing EHR by Timestamp From The Past As Parameter (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    create EHR and commit a composition for versioned composition tests

    # comment: valid timestamp format, but points to time in the past
    ${date} = 	Get Current Date    increment=-7 days    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404


7f. Get Composition via Versioned Composition Of Existing EHR by Timestamp From The Future As Parameter (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    create EHR and commit a composition for versioned composition tests

    # comment: valid timestamp format, but points to time in the past
    ${date} = 	Get Current Date    increment=7 days    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get version of versioned composition of EHR by UID and time    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
