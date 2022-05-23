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

Resource        ../../_resources/keywords/composition_keywords.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags      COMPOSITION_get_versioned



*** Test Cases ***
1. Get Revision History of Versioned Composition Of Existing EHR (JSON)
    [Documentation]    Simple test

    create EHR and commit a composition for versioned composition tests

    get revision history of versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    ${length} =    Get Length    ${response.body} 	
    Should Be Equal As Integers 	${length} 	1

    ${item1} =    Get From List    ${response.body}    0
    Should Be Equal As Strings    ${version_uid}    ${item1.version_id.value}


2. Get Revision History Of Versioned Composition Of Existing EHR With Two Composition Versions (JSON)
    [Tags]      not-ready
    [Documentation]    Testing with two versions, so the result should list two history entries.
    ...     Checks if versions are listed in desc order -> Latest modified first.
    ...     Doc: https://specifications.openehr.org/releases/RM/latest/common.html#_revision_history_class

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    get revision history of versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    ${length} =    Get Length    ${response.body} 	
    Should Be Equal As Integers 	${length} 	2

    ${item1} =    Get From List    ${response.body}    0
    Should Be Equal As Strings    ${version_uid[0:-1]}2    ${item1.version_id.value}

    ${item2} =    Get From List    ${response.body}    1
    Should Be Equal As Strings    ${version_uid[0:-1]}1    ${item2.version_id.value}
    [Teardown]      TRACE JIRA ISSUE    CDR-413


3. Get Correct Ordered Revision History of Versioned Composition Of Existing EHR With Two Composition Versions (JSON)
    [Tags]      not-ready
    [Documentation]     Testing with two versions like above, but checking the response more thoroughly.

    create EHR and commit a composition for versioned composition tests

    update a composition for versioned composition tests

    get revision history of versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    200
    ${length} =    Get Length    ${response.body} 	
    Should Be Equal As Integers 	${length} 	2

    # comment: Attention: the following code is depending on the order of the array!
    ${item1} =    Get From List    ${response.body}    0
    Should Be Equal As Strings    ${version_uid[0:-1]}2    ${item1.version_id.value}
    # comment: check if change type is "creation"
    ${audit1} =    Get From List    ${item1.audits}    0
    Should Be Equal As Strings    creation    ${audit1.change_type.value}
    # comment: save timestamp to compare later
    ${timestamp1} = 	Convert Date    ${audit1.time_committed.value}    result_format=%Y-%m-%dT%H:%M:%S.%f

    ${item2} =    Get From List    ${response.body}    1
    Should Be Equal As Strings    ${version_uid[0:-1]}1    ${item2.version_id.value}
    # comment: check if change type is "modification"
    ${audit2} =    Get From List    ${item2.audits}    0
    Should Be Equal As Strings    modification    ${audit2.change_type.value}
    # comment: save timestamp2, too.
    ${timestamp2} = 	Convert Date    ${audit2.time_committed.value}    result_format=%Y-%m-%dT%H:%M:%S.%f


    # comment: check if this one is newer/bigger/higher than the creation timestamp.
    ${timediff} = 	Subtract Date From Date 	${timestamp1} 	${timestamp2}

    # comment: Idea here: newer/higher timestamp - older/lesser timestamp = number larger than 0 IF correct
    Should Be True 	${timediff} > 0
    [Teardown]      TRACE JIRA ISSUE    CDR-413


4. Get Revision History of Versioned Composition Of Non-Existing EHR (JSON)
    [Documentation]    Simple test

    create EHR and commit a composition for versioned composition tests
    create fake EHR

    get revision history of versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404


5. Get Revision History of Versioned Composition Of Non-Existing Composition (JSON)
    [Documentation]    Simple test

    create EHR and commit a composition for versioned composition tests
    create fake composition

    get revision history of versioned composition of EHR by UID    ${versioned_object_uid}
    Should Be Equal As Strings    ${response.status}    404