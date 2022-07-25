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
Documentation   EVENT Trigger Integration Tests
Metadata        TOP_TEST_SUITE    EVENT_TRIGGER_TESTS

Resource        ../_resources/keywords/event_trigger_keywords.robot
Resource        ../_resources/suite_settings.robot


*** Test Cases ***
Create And Get Event Trigger By Different Criteria
    [Documentation]     - Creates event trigger.
    ...                 - Checks status code to be 200.
    ...                 - Get event trigger by uuid and expects status code 200.
    ...                 - Get event trigger by id and expects status code 200.
    ...                 ${\n}Event trigger plugin should be available in EHRBase.
    Commit Event Trigger    main_event_trigger.json
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200
    Get Event Trigger By Criteria   ${event_id}     200


Get All Created Event Triggers
    [Documentation]     - Get all Event Triggers and check status code to be 200.
    ...                 - Validate that number of Event Triggers > 3.
    Load Many Event Triggers And Store In Lists
    Get All Event Triggers

Delete Event Trigger
    [Documentation]     - Create Event Trigger.
    ...                 - Get Event Trigger using uuid and expect 200.
    ...                 - Delete Event Trigger using uuid.
    ...                 - Get Event Trigger using uuid and expect 404.
    Commit Event Trigger    main_event_trigger.json
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200
    Delete Event Trigger By UUID    ${event_uuid}
    Get Event Trigger By Criteria   ${event_uuid}   404


*** Keywords ***
Load Many Event Triggers And Store In Lists
    [Documentation]     - Commit n numbers of event triggers.
    ...                 - n - is provided to this keyword as digit argument.
    ...                 - Store event uuids and event ids in 2 separate lists
    [Arguments]         ${nr_of_event_triggers}=3
    ${event_uuids_list}      Create List
    ${event_ids_list}        Create List
    FOR  ${index}   IN RANGE  0   ${nr_of_event_triggers}
        Commit Event Trigger    main_event_trigger.json
        Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}    console=yes
        Append To List      ${event_uuids_list}      ${event_uuid}
        Append To List      ${event_ids_list}      ${event_id}
    END
    Set Test Variable     ${uuids_list}     ${event_uuids_list}
    Set Test Variable     ${ids_list}       ${event_ids_list}
