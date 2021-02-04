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
Metadata    Version    0.1.0
Metadata    Author    *Wladislaw Wagner*
Metadata    Author    *Jake Smolka*
Metadata    Created    2021.01.26

Documentation   Preconditions:
...                 An EHR with known ehr_id should exist.
...
...             Postconditions:
...                 None
...
...             Flow:
...                 1. Invoke the get EHR_STATUS service by the existing ehr_id
...                 2. The result should be positive and retrieve a correspondent EHR_STATUS.
...                    The EHR_STATUS internal information should match the rules in which
...                    the EHR was created (see test flow Create EHR) and those should be verified:
...
...                    a) has or not a subject_id
...                    b) has correct value for is_modifiable
...                    c) has correct value for is_queryable
Metadata        TOP_TEST_SUITE    EHR_STATUS
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
1. Get Versioned Status Of Existing EHR by Time Without Query (JSON)
    # Simple test without query param

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201	

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}


2. Get Versioned Status Of Existing EHR by Time With Query (JSON)
    # Test with query param

    Import Library    DateTime

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # set the query parameter to current data
    ${date} = 	Get Current Date
    ${date} = 	Replace String 	${date} 	${space} 	T       # manually convert robot timestamp to openEHR REST spec timestamp
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}


3. Get Versioned Status Of Existing EHR by Time With Query (JSON)
    # Test with and without query param and multiple versions
    [Tags]    XXX

    Import Library    DateTime

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201
    # comment: save orginal version uid
    ${original_id} =  Set Variable  ${ehrstatus_uid}

            # # save timestamp and convert to robot timestamp
            # ${timestamp} =    Convert Date    ${response.body.time_created.value}    result_format=%Y-%m-%dT%H:%M:%S
            # log    ${timestamp}
            # # ${timestamp} = 	Replace String 	${timestamp} 	, 	.
            # # ${timestamp} = 	Replace String 	${timestamp} 	T 	${space}
    Sleep    1

    capture point in time  after_ehr_creation
    Log    ${time_after_ehr_creation}
    Sleep    3

    update EHR: set ehr_status is_queryable    ${TRUE}
    check response of 'update EHR' (JSON)
    Sleep    1

    # comment: 1. check if latest version gets returned without parameter
    Log    GET VERSIONED EHR_STATUS (LATEST)
    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid[0:-1]}2    ${response.body.uid.value}


    # comment: 2. check if current time returns latest version too
    Log    GET VERSIONED EHR_STATUS (LATEST - BY CURRENT TIME)
    # comment: set the query parameter to current date/time
    ${current_time} =    Get Current Date    result_format=%Y-%m-%dT%H:%M:%S    # openEHR REST spec conformant timestamp
    Set Test Variable 	&{query} 	version_at_time=${current_time}     # set query as dictionary
    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid[0:-1]}2    ${response.body.uid.value}

    # comment: 3. check if original timestamp returns original version
    Log    GET VERSIONED EHR_STATUS (ORIGINAL - BY CREATION TIME)
    # first add some time to timestamp so it actually points to time after the creation itself

            # ${timestamp} = 	Add Time To Date 	${timestamp} 	1 s    result_format=%Y-%m-%dT%H:%M:%S
            # # ${timestamp} = 	Replace String 	${timestamp} 	${space} 	T       # manually convert robot timestamp to openEHR REST spec timestamp
    
    Set Test Variable 	&{query} 	version_at_time=${time_after_ehr_creation}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${original_id}    ${response.body.uid.value}



4. Get Versioned Status Of Existing EHR by Time Check Lifecycle State (JSON)
    # Simple, but checking lifecycle state

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}
    Should Be Equal As Strings    complete   ${response.body.lifecycle_state.value}


5a. Get Versioned Status Of Existing EHR by Time Check Preceding Version (JSON)
    # Simple, but checking preceding version uid - with one version

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201
    
    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${ehrstatus_uid}    ${response.body.uid.value}
    Should Not Contain  ${response.body}  preceding_version_uid


5b. Get Versioned Status Of Existing EHR by Time Check Preceding Version (JSON)
    # Simple, but checking preceding version uid - with two versions

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201
    # save orginal version uid
    Set Test Variable  ${original_id}  ${ehrstatus_uid}

    update EHR: set ehr_status is_queryable    ${TRUE}
    check response of 'update EHR' (JSON)

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    ${original_id}    ${response.body.preceding_version_uid.value}


6. Get Versioned Status Of Existing EHR by Time Check Data (JSON)
    # Simple, but checking object data - with two versions

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    update EHR: set ehr_status is_queryable    ${FALSE}
    check response of 'update EHR' (JSON)

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200
    Should Be Equal As Strings    false    ${response.body.data.is_queryable}  ignore_case=True


7a. Get Versioned Status Of Existing EHR by Time With Parameter Check (JSON)
    # Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # a. valid: set the query parameter to current data
    ${date} = 	Get Current Date
    ${date} = 	Replace String 	${date} 	${space} 	T       # manually convert robot timestamp to openEHR REST spec timestamp
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    200


7b. Get Versioned Status Of Existing EHR by Time With Parameter Check (JSON)
    # Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # b. invalid: wrong timestamp format - 400
    ${date} = 	Get Current Date
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    400


7c. Get Versioned Status Of EHR by Time With Parameter Check (JSON)
    # Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # c. invalid: wrong EHR - 404
    # take the SYSTEM_ID UID to guarantee this ID doesn't match the EHR ID
    Set Test Variable 	${ehr_id} 	${response.body.system_id.value}

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    404

7d. Get Versioned Status Of Existing EHR by Time With Parameter Check (JSON)
    # Checking for expected responses with and without valid parameters

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    # d. valid but invalid: valid timestamp format, but points to time in the past
    ${date} = 	Get Current Date
    ${date} = 	Subtract Time From Date 	${date} 	7 days
    ${date} = 	Replace String 	${date} 	${space} 	T       # manually convert robot timestamp to openEHR REST spec timestamp
    Set Test Variable 	&{query} 	version_at_time=${date}     # set query as dictionary

    get versioned ehr_status of EHR by time
    Should Be Equal As Strings    ${response.status}    404




