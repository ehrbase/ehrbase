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

Suite Setup     Precondition

*** Variables ***
${exchange_name}    etexchange
${queue_name}       robot_queue
${routing_key}      et

*** Test Cases ***
Validate That Message Is Registered In RabbitMQ For New Composition
    [Documentation]     Validate that message is present in RabbitMQ queue.
    ...                 Message appears after commit of composition.
    ...                 Message presence is checked by searching for {compositionUid}, after commit composition.
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.xml.flat.json
    check the successful result of commit composition   nesting
    ${returnedQuery}     Get Message From Queue RabbitMQ      queue_name=robot_queue
    [Teardown]          Run Keywords    Delete Event Trigger By UUID    ${event_uuid}   200     AND
    ...     Delete Exchange By Name     etexchange      AND
    ...     Delete Queue By Name        robot_queue

*** Keywords ***
Precondition
    Create Exchange Queue And Binding In RabbitMQ
    Upload OPT    /nested/nested.opt
    create EHR
    Commit Event Trigger    main_event_trigger.json
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200

Create Exchange Queue And Binding In RabbitMQ
    Create Exchange RabbitMQ    exchange_name=${exchange_name}
    Create Queue RabbitMQ       queue_name=${queue_name}
    Bind Exchange To Queue      exchange_name=${exchange_name}
    ...     queue_name=${queue_name}     routing_key=${routing_key}