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
1. Get Revision History of Versioned Status Of Existing EHR (JSON)
    [Documentation]    Simple test

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    get revision history of versioned ehr_status of EHR
    Should Be Equal As Strings    ${response.status}    200
    ${length} =    Get Length    ${response.body} 	
    Should Be Equal As Integers 	${length} 	1

    ${item1} =    Get From List    ${response.body}    0
    Should Be Equal As Strings    ${ehrstatus_uid}    ${item1.version_id.value}


2. Get Revision History of Versioned Status Of Existing EHR With Two Status Versions (JSON)
    [Documentation]    Testing with two versions, so the result should list two history entries.

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    update EHR: set ehr_status is_queryable    ${TRUE}
    check response of 'update EHR' (JSON)

    get revision history of versioned ehr_status of EHR
    Should Be Equal As Strings    ${response.status}    200
    ${length} =    Get Length    ${response.body} 	
    Should Be Equal As Integers 	${length} 	2

    ${item1} =    Get From List    ${response.body}    0
    Should Be Equal As Strings    ${ehrstatus_uid}    ${item1.version_id.value}

    ${item2} =    Get From List    ${response.body}    1
    Should Be Equal As Strings    ${ehrstatus_uid[0:-1]}2    ${item2.version_id.value}


3. Get Correct Ordered Revision History of Versioned Status Of Existing EHR With Two Status Versions (JSON)
    [Documentation]     Testing with two versions like above, but checking the response more thoroughly.
    [Tags]              not-ready   458 

    prepare new request session    JSON    Prefer=return=representation

    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    update EHR: set ehr_status is_queryable    ${TRUE}
    check response of 'update EHR' (JSON)

    get revision history of versioned ehr_status of EHR
    Should Be Equal As Strings    ${response.status}    200
    ${length} =    Get Length    ${response.body} 	
    Should Be Equal As Integers 	${length} 	2

    # comment: Attention: the following code is depending on the order of the array!
    ${item1} =    Get From List    ${response.body}    0
    Should Be Equal As Strings    ${ehrstatus_uid}    ${item1.version_id.value}
    # comment: check if change type is "creation"
    ${audit1} =    Get From List    ${item1.audits}    0
    Should Be Equal As Strings    creation    ${audit1.change_type.value}
    # comment: save timestamp to compare later
    ${timestamp1} = 	Convert Date    ${audit1.time_committed.value}    result_format=%Y-%m-%dT%H:%M:%S.%f

    ${item2} =    Get From List    ${response.body}    1
    Should Be Equal As Strings    ${ehrstatus_uid[0:-1]}2    ${item2.version_id.value}
    # comment: check if change type is "modification"
    ${audit2} =    Get From List    ${item2.audits}    0
    Should Be Equal As Strings    modification    ${audit2.change_type.value}
    # comment: save timestamp2, too.
    ${timestamp2} = 	Convert Date    ${audit2.time_committed.value}    result_format=%Y-%m-%dT%H:%M:%S.%f



    # comment: check if this one is newer/bigger/higher than the creation timestamp.
    ${timediff} = 	Subtract Date From Date 	${timestamp2} 	${timestamp1}

    # comment: Idea - newer/higher timestamp - older/lesser timestamp = number larger than 0 IF correct
    Should Be True 	${timediff} > 0
    [Teardown]    TRACE GITHUB ISSUE    458    bug
