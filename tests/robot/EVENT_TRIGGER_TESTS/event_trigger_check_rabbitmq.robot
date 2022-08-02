# Copyright (c) 2022 Vladislav Ploaia (Vitagroup - CDR Core Team)
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
Documentation   EVENT Trigger Integration Tests With Validation In RabbitMQ
Metadata        TOP_TEST_SUITE    EVENT_TRIGGER_TESTS

Resource        ../_resources/keywords/event_trigger_keywords.robot
Resource        ../_resources/keywords/rabbitmq_keywords.robot
Resource        ../_resources/suite_settings.robot
Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/aql_query_keywords.robot
Resource        ../_resources/keywords/event_trigger_mock_keywords.robot


*** Variables ***
${exchange_name}    etexchange
${queue_name}       robot_queue
${routing_key}      et
${MOCK_EVENT_TRIGGER_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/mocks/event_trigger

*** Test Cases ***
Validate That Message Is Registered In RabbitMQ For New Composition - Event Trigger Active
    [Documentation]     Validate that message is present in RabbitMQ queue.
    ...                 Message appears after commit of composition.
    ...                 Message presence is checked by searching for {compositionUid}, after commit composition.
    [Setup]             Precondition
    commit composition  format=FLAT
    ...                 composition=nested.en.v1__full.xml.flat.json
    check the successful result of commit composition   nesting
    ${returnedQuery}    Get Message From Queue RabbitMQ     queue_name=robot_queue
    [Teardown]          Run Keyword And Return Status       Postcondition

Validate That Message Is Not Registered In RabbitMQ For New Composition - Event Trigger Inactive
    [Documentation]     Validate that message is not present in RabbitMQ queue.
    ...                 If Event Trigger state value is inactive, queue will not receive any message.
    ...                 Trigger is Commit Composition.
    ...                 Absence of message is expected from RabbitMQ queue.
    Create Exchange Queue And Binding In RabbitMQ
    Upload OPT    /nested/nested.opt
    create EHR
    Commit Event Trigger    main_event_trigger.json     inactive
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200
    commit composition  format=FLAT
    ...                 composition=nested.en.v1__full.xml.flat.json
    check the successful result of commit composition   nesting
    ${expectedError}    Set Variable    Queue - robot_queue - does not contain any message.
    ${returnedQuery}    Run Keyword And Expect Error   ${expectedError}
    ...     Get Message From Queue RabbitMQ     queue_name=robot_queue
    Should Be Equal As Strings      ${returnedQuery}    ${expectedError}
    ##########
    Log     Delete Event Trigger, create new one with active state, commit compo and check presence of message in queue.
    Delete Event Trigger By UUID    ${event_uuid}   200
    Commit Event Trigger    main_event_trigger.json     active
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.xml.flat.json
    check the successful result of commit composition   nesting
    ${returnedQuery}    Get Message From Queue RabbitMQ     queue_name=robot_queue
    [Teardown]          Run Keyword And Return Status       Postcondition

Validate That HTTP Trigger Event Is Created
    [Documentation]   In progress.
    [Tags]      not-ready
    #Create Exchange Queue And Binding In RabbitMQ
    Upload OPT    /nested/nested.opt
    create EHR
    Commit Event Trigger    http_trigger_event_trigger.json     active
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200
    ${query}=           Catenate
    ...                 SELECT
    ...                     c/uid/value as diastolic
    ...                 FROM
    ...                     EHR e
    ...                 CONTAINS
    ...                     COMPOSITION c
    Set Test Variable    ${payload}    {"q": "${query}"}
    Create Sessions
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.xml.flat.json
    check the successful result of commit composition   nesting
    Change Mock Request Json KeyValue And Save Back To File
    Change Mock Response Json KeyValue And Save Back To File
    POST Create Mock Expectation Event Trigger
    ...     ${MOCK_EVENT_TRIGGER_PATH}/path_success_request.json
    ...     ${MOCK_EVENT_TRIGGER_PATH}/path_success_response.json
    ...     200
    POST /query/aql (REST)    JSON
    Integer    response status    200
    ${resultQ}      Set Variable    ${response body["q"]}
    Should Be Equal As Strings    ${resultQ}     ${query}
    ${resultRows}   Set Variable    ${response body["rows"]}
    Check That Result Rows Contains Composition Uid
    ...     ${resultRows}       ${compositionUid}
    #Send POST Endpoint Expect Success
    #...     ${TEST_PATH_EVENT_TRIGGER_ENDPOINT}
    #...     ${MOCK_EVENT_TRIGGER_PATH}/path_success_request.json
    [Teardown]      Run Keywords
    ...     Delete Event Trigger By UUID    ${event_uuid}    AND
    ...     Reset Mock Server

*** Keywords ***
Precondition
    Create Exchange Queue And Binding In RabbitMQ
    Upload OPT    /nested/nested.opt
    create EHR
    Commit Event Trigger    main_event_trigger.json     active
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200

Postcondition
    [Documentation]     - Delete Event Trigger.
    ...                 - Delete RabbitMQ Exchange.
    ...                 - Delete RabbitMQ Queue.
    Run Keywords    Delete Event Trigger By UUID    ${event_uuid}   200     AND
    ...     Delete Exchange By Name         etexchange      AND
    ...     Delete Queue By Name            robot_queue

Create Exchange Queue And Binding In RabbitMQ
    Create Exchange RabbitMQ    exchange_name=${exchange_name}
    Create Queue RabbitMQ       queue_name=${queue_name}
    Bind Exchange To Queue      exchange_name=${exchange_name}
    ...     queue_name=${queue_name}     routing_key=${routing_key}

Create Sessions
    Create Session          server      ${MOCK_URL}
    Create Mock Session     ${MOCK_URL}

Reset Mock Server
    Dump To Log
    Reset All Requests

Change Mock Response Json KeyValue And Save Back To File
    [Documentation]     Updates $.q and $.rows[0][0] values and save back to path_success_response.json file.
    ${jsonContent}           Load Json From File     ${MOCK_EVENT_TRIGGER_PATH}/path_success_response.json
    ${compoWithoutVersion}   Remove String       ${compositionUid}    ::${CREATING_SYSTEM_ID}::1
    ${json_object}          Update Value To Json	${jsonContent}
    ...             $.q        Select c/uid/value as diastolic from EHR e contains COMPOSITION c where (e/ehr_id/value = '${ehr_id}' and c/uid/value = '${compoWithoutVersion}')
    ${json_object}          Update Value To Json	${jsonContent}
    ...             $.rows[0][0]
    ...             ${compositionUid}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${MOCK_EVENT_TRIGGER_PATH}/path_success_response.json    ${json_str}

Change Mock Request Json KeyValue And Save Back To File
    [Documentation]     Updates $.q and save back to path_success_request.json file.
    ${jsonContent}           Load Json From File     ${MOCK_EVENT_TRIGGER_PATH}/path_success_request.json
    ${compoWithoutVersion}   Remove String       ${compositionUid}    ::${CREATING_SYSTEM_ID}::1
    ${json_object}          Update Value To Json	${jsonContent}
    ...             $.q        Select c/uid/value as diastolic from EHR e contains COMPOSITION c where (e/ehr_id/value = '${ehr_id}' and c/uid/value = '${compoWithoutVersion}')
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${MOCK_EVENT_TRIGGER_PATH}/path_success_request.json    ${json_str}

Check That Result Rows Contains Composition Uid
    [Arguments]     ${resultRows}   ${expectedString}
    ${listWithCompositionUIds}      Create List
    FOR     ${el}      IN      @{resultRows}
        Append To List      ${listWithCompositionUIds}     ${el}[0]
    END
    List Should Contain Value       ${listWithCompositionUIds}      ${expectedString}