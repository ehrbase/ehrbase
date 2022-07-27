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
Documentation    RabbitMQ Specific Keywords
Resource   ../suite_settings.robot


*** Keywords ***
Check If Session With RabbitMQ Endpoint Exists
    [Documentation]     Checks if RabbitMQ session exists.
    ...     If it does not exist, it will create the session.
    ...     - Call this keyword before sending POST, GET requests.
    ${sessionExists}     Session Exists      rabbitmqsession
    IF      ${sessionExists} == ${FALSE}
        ${headers}  Create Dictionary   Authorization=Basic Z3Vlc3Q6Z3Vlc3Q=
        Create Session      rabbitmqsession    ${RABBITMQURL}
        ...     debug=2     verify=True   headers=${headers}
    END

Create Exchange RabbitMQ
    [Documentation]     - 1. Creates Exchange in RabbitMQ.
    ...                 - Exchange name is provided through argument.
    ...                 - ENDPOINT: /exchanges/%2F/{exchange_name}.
    ...                 - Method: PUT
    ...                 - 2. Check if Exchange item is present.
    ...                 - ENDPOINT: /exchanges/%2F/{exchange_name}
    ...                 - Method: GET
    ...                 - Check if response contains {exchange_name} value
    [Arguments]     ${exchange_name}=etexchange
    Check If Session With RabbitMQ Endpoint Exists
    ${empty_args}       Create Dictionary
    ${dataContent}      Create Dictionary
    ...     vhost=/         name=${exchange_name}    type=topic
    ...     durable=true    auto_delete=false
    ...     internal=false  arguments=${empty_args}
    ${json_string}      Convert JSON to string    ${dataContent}
    ${resp}     PUT on session      rabbitmqsession
    ...     /exchanges/%2F/${exchange_name}  data=${json_string}
        Should Be Equal As Strings      ${resp.status_code}     201
    ${queryParams}      Create Dictionary      name=${exchange_name}
    ${resp}     GET on session      rabbitmqsession
    ...     /exchanges/%2F/${exchange_name}
        Should Be Equal As Strings      ${resp.status_code}     200
        Should Be Equal As Strings      ${resp.json()['name']}   ${exchange_name}
        Should Be Equal As Strings      ${resp.json()['type']}   topic

Create Queue RabbitMQ
    [Documentation]     - Creates Queue in RabbitMQ.
    ...                 - Queue name is provided through argument {queue_name}.
    ...                 - ENDPOINT:
    ...                 - Method: PUT /queues/%2F/{queue_name}
    [Arguments]     ${queue_name}=robot_queue
    Check If Session With RabbitMQ Endpoint Exists
    ${dataContentArgs}  Create Dictionary   x-queue-type=classic
    ${dataContent}      Create Dictionary
    ...     vhost=/     name=${queue_name}     durable=true
    ...     auto_delete=false   arguments=${dataContentArgs}
    ${json_string}      Convert JSON to string    ${dataContent}
    ${resp}     PUT on session      rabbitmqsession
    ...     /queues/%2F/${queue_name}
    Should Be Equal As Strings      ${resp.status_code}     201


Bind Exchange To Queue
    [Documentation]     - Bind Exchange To Queue.
    ...                 - 3 arguments with default values: {exchange_name}, {queue_name}, {routing_key}.
    ...                 - ENDPOINT: /bindings/%2F/e/{exchange_name}/q/{queue_name}
    ...                 - Method: POST
    [Arguments]     ${exchange_name}=etexchange    ${queue_name}=robot_queue    ${routing_key}=et
    Check If Session With RabbitMQ Endpoint Exists
    ${empty_args}       Create Dictionary
    ${dataContent}      Create Dictionary
    ...     vhost=/     destination=${queue_name}   destination_type=q
    ...     source=${exchange_name}     routing_key=${routing_key}      arguments=${empty_args}
    ${resp}     POST on session      rabbitmqsession
    ...     /bindings/%2F/e/${exchange_name}/q/${queue_name}    json=${dataContent}
    Should Be Equal As Strings      ${resp.status_code}     201

Get Message From Queue RabbitMQ
    [Documentation]     - Get message from queue, where queue is indentified by name.
    ...                 - 2 arguments (optional) {queue_name}, {searchCriteria}.
    ...                 - DEPENDENCY: `check the successful result of commit composition`, of {compositionUid} variable.
    ...                 - {searchCriteria} is searched into the last message from the queue.
    ...                 - If {searchCriteria} is not found, error will be returned. Keyword will be failed.
    ...                 - ENDPOINT: /queues/%2F/{queue_name}/get
    ...                 - Method: POST
    ...                 - Returns message payload.
    [Arguments]     ${queue_name}=robot_queue   ${searchCriteria}=${compositionUid}
    Check If Session With RabbitMQ Endpoint Exists
    ${dataContent}      Create Dictionary
    ...     count=50    ackmode=ack_requeue_true    encoding=auto
    ${resp}     POST on session      rabbitmqsession
    ...     /queues/%2F/${queue_name}/get       json=${dataContent}
        Should Be Equal As Strings      ${resp.status_code}     200
        ${lengthOfResponse}     Get Length     ${resp.content}  #check if response payload is not empty
        Run Keyword If      ${lengthOfResponse} < 5
        ...     Fail    Queue - ${queue_name} - does not contain any message.
        ${full_payload}         Set Variable    ${resp.json()}
        ${maxNrOfMessages}      Set Variable    ${resp.json()[0]['message_count']}
        Should Contain     ${resp.json()[${maxNrOfMessages}]['payload']}     ${searchCriteria}
        ...     msg='${searchCriteria}' is not found in Queue - ${queue_name} - with message_count=${maxNrOfMessages}.
        Set Test Variable       ${payload}      ${resp.json()[0]['payload']}
        [return]        ${payload}

Delete Exchange By Name
    [Documentation]     - Delete RabbitMQ Exchange by name.
    ...                 - Name argument should be specified, to identify Exchange by name and delete it.
    ...                 - ENDPOINT: /exchanges/%2F/{exchange_name}
    ...                 - Method: DELETE
    [Arguments]     ${exchange_name}=etexchange
    #http://localhost:15672/api/exchanges/%2F/etexchange
    #DELETE
    #payload: {"vhost":"/","name":"etexchange"}
    Check If Session With RabbitMQ Endpoint Exists
    ${dataContent}      Create Dictionary
    ...     vhost=/     name=${exchange_name}
    ${resp}     DELETE on session      rabbitmqsession
    ...     /exchanges/%2F/${exchange_name}       json=${dataContent}
    Should Be Equal As Strings      ${resp.status_code}     204     Exchange - ${exchange_name} - not deleted.

Delete Queue By Name
    [Documentation]     - Delete RabbitMQ Queue by name.
    ...                 - Name argument should be specified, to identify Queue by name and delete it.
    ...                 - ENDPOINT: /exchanges/%2F/{queue_name}
    ...                 - Method: DELETE
    [Arguments]     ${queue_name}=robot_queue
    Check If Session With RabbitMQ Endpoint Exists
    ${dataContent}      Create Dictionary
    ...     vhost=/     name=${queue_name}      mode=delete
    ${resp}     DELETE on session      rabbitmqsession
    ...     /queues/%2F/${queue_name}       json=${dataContent}


