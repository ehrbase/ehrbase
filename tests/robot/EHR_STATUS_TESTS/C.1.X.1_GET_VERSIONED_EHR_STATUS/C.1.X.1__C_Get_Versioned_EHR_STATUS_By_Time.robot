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

Force Tags



*** Test Cases ***
1. Get Versioned Status Of Existing EHR by Time Without Query (JSON)
    [Documentation]    Simple test without query param

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201	

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}


2. Get Versioned Status Of Existing EHR by Time With Query (JSON)
    [Documentation]    Test with query param

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # comment: set the query parameter to current data and format as openEHR REST spec conformant timestamp
    ${date} = 	Get Current Date    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}


3. Get Versioned Status Of Existing EHR by Time With Query (JSON)
    [Documentation]    Test with and without query param and multiple versions

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201
    # comment: save orginal version uid
    ${original_id} =  Set Variable  ${ehrstatus_uid}

    capture point in time  after_ehr_creation
    Log    ${time_after_ehr_creation}

    update EHR: set ehr_status is_queryable    ${TRUE}
    check response of 'update EHR' (JSON)

    # comment: 1. check if latest version gets returned without parameter
    Log    GET VERSIONED EHR_STATUS (LATEST)
    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid[0:-1]}2    ${response.body.uid.value}

    # comment: 2. check if current time returns latest version too
    Log    GET VERSIONED EHR_STATUS (LATEST - BY CURRENT TIME)
    # comment: set the query parameter to current date/time and format as openEHR REST spec conformant timestamp
    ${current_time} =    Get Current Date    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${current_time}     # set query as dictionary
    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid[0:-1]}2    ${response.body.uid.value}

    # comment: 3. check if original timestamp returns original version
    Log    GET VERSIONED EHR_STATUS (ORIGINAL - BY CREATION TIME)
    Set Test Variable 	&{query} 	version_at_time=${time_after_ehr_creation}     # set query as dictionary
    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${original_id}    ${response.body.uid.value}


4. Get Versioned Status Of Existing EHR by Time Check Lifecycle State (JSON)
    [Documentation]    Simple, but checking lifecycle state

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}
    Should Be Equal As Strings    complete   ${response.body.lifecycle_state.value}


5a. Get Versioned Status Of Existing EHR by Time Check Preceding Version (JSON)
    [Documentation]    Simple, but checking preceding version uid - with one version

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201
    
    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}
    Should Not Contain  ${response.body}  preceding_version_uid


5b. Get Versioned Status Of Existing EHR by Time Check Preceding Version (JSON)
    [Documentation]    Simple, but checking preceding version uid - with two versions

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201
    # comment: save orginal version uid
    Set Test Variable  ${original_id}  ${ehrstatus_uid}

    update EHR: set ehr_status is_queryable    ${TRUE}
    check response of 'update EHR' (JSON)

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${original_id}    ${response.body.preceding_version_uid.value}


6. Get Versioned Status Of Existing EHR by Time Check Data (JSON)
    [Documentation]    Simple, but checking object data - with two versions

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    update EHR: set ehr_status is_queryable    ${FALSE}
    check response of 'update EHR' (JSON)

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    false    ${response.body.data.is_queryable}  ignore_case=True


7a. Get Versioned Status Of Existing EHR by Time With Parameter Check (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # comment: set the query parameter to current data in openEHR REST spec conformant timestamp
    ${date} = 	Get Current Date    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200


7b. Get Versioned Status Of Existing EHR With Invalid Timestamp As Parameter (JSON)
    [Documentation]    Checking for expected responses with invalid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # comment: generate a timestamp which is considered invalid by openEHR REST Spec
    ${date} = 	Get Current Date
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    400


7c. Get Versioned Status Of Non-Existent EHR by Time With Parameter Check (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create fake EHR
    Should Be Equal As Strings    ${response.status}    201

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    404


7d. Get Versioned Status Of Existing EHR by Timestamp From The Past As Parameter (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # comment: valid timestamp format, but points to time in the past
    ${date} = 	Get Current Date    increment=-7 days    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    404


7e. Get Versioned Status Of Existing EHR by Timestamp From The Future As Parameter (JSON)
    [Documentation]    Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # comment: valid timestamp format, but points to time in the past
    ${date} = 	Get Current Date    increment=7 days    result_format=%Y-%m-%dT%H:%M:%S.%f
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
