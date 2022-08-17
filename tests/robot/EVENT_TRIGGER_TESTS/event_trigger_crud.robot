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


*** Variables ***
${event_trigger_file}       main_event_trigger.json
${temp_file_invalid_aql_in_event_trigger}       temp_event_trigger_with_invalid_aql.json


*** Test Cases ***
Create And Get Event Trigger By Different Criteria
    [Documentation]     - Creates event trigger.
    ...                 - Checks status code to be 200.
    ...                 - Get event trigger by uuid and expects status code 200.
    ...                 - Get event trigger by id and expects status code 200.
    ...                 ${\n}Event trigger plugin should be available in EHRBase.
    [Tags]      Positive
    Commit Event Trigger    ${event_trigger_file}
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200
    Get Event Trigger By Criteria   ${event_id}     200
    [Teardown]      Delete Event Trigger By UUID    ${event_uuid}   200

Get All Created Event Triggers
    [Documentation]     - Get all Event Triggers and check status code to be 200.
    ...                 - Validate that number of Event Triggers > 3.
    [Tags]      Positive
    Load Many Event Triggers And Store In Lists
    Get All Event Triggers
    [Teardown]      Delete All Created Event Triggers   ${uuids_list}

Delete Event Trigger
    [Documentation]     - Create Event Trigger.
    ...                 - Get Event Trigger using uuid and expect 200.
    ...                 - Delete Event Trigger using uuid.
    ...                 - Get Event Trigger using uuid and expect 404.
    [Tags]      Positive    Negative
    Commit Event Trigger    ${event_trigger_file}
    Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}
    Get Event Trigger By Criteria   ${event_uuid}   200
    Delete Event Trigger By UUID    ${event_uuid}   200
    Get Event Trigger By Criteria   ${event_uuid}   404
    Delete Event Trigger By UUID    ${event_uuid}   404

Check That Event Trigger Cannot Be Created With Invalid AQL
    [Documentation]     - Check that Event Trigger cannot be created if AQL query is wrong.
    ...                 - 400 status code must be returned with explicit error message.
    [Tags]      Negative
    ${newFileInvalidEventTrigger}   Change Event Trigger Json KeyValue And Save Back To New File
    ...     $.definition.rules[0]['high diastolic'].when.aql
    ...     select c/uid/value as diastolic from EHR e contains COMPOSITION
    Commit Event Trigger    ${newFileInvalidEventTrigger}       status_code=400
    Log     ${response.content}
    Should Be Equal As Strings
    ...     Failed to parse[select c/uid/value as diastolic from EHR e contains COMPOSITION] on rule[high diastolic]
    ...     ${response.content}
    ${newFileInvalidEventTrigger}   Change Event Trigger Json KeyValue And Save Back To New File
    ...     $.definition.rules[0]['high diastolic'].when.aql
    ...     select c/uid/value as diastolic from contains COMPOSITION c
    Commit Event Trigger    ${newFileInvalidEventTrigger}       status_code=400
    Log     ${response.content}
    Should Be Equal As Strings
    ...     Failed to parse[select c/uid/value as diastolic from contains COMPOSITION c] on rule[high diastolic]
    ...     ${response.content}
    ${newFileInvalidEventTrigger}   Change Event Trigger Json KeyValue And Save Back To New File
    ...     $.definition.rules[0]['high diastolic'].when.aql
    ...     select c/uid/value as diastolic
    Commit Event Trigger    ${newFileInvalidEventTrigger}       status_code=400
    Log     ${response.content}
    Should Be Equal As Strings
    ...     Failed to parse[select c/uid/value as diastolic] on rule[high diastolic]
    ...     ${response.content}
    ${newFileInvalidEventTrigger}   Change Event Trigger Json KeyValue And Save Back To New File
    ...     $.definition.rules[0]['high diastolic'].when.aql
    ...     select
    Commit Event Trigger    ${newFileInvalidEventTrigger}       status_code=400
    Log     ${response.content}
    Should Be Equal As Strings
    ...     Failed to parse[select] on rule[high diastolic]
    ...     ${response.content}
    [Teardown]      Run Keyword And Return Status   Remove File     ${VALID EVENT TRIGGER DATA SETS}/create/${newFileInvalidEventTrigger}

*** Keywords ***
Load Many Event Triggers And Store In Lists
    [Documentation]     - Commit n numbers of event triggers.
    ...                 - n - is provided to this keyword as digit argument.
    ...                 - Store event uuids and event ids in 2 separate lists
    [Arguments]         ${nr_of_event_triggers}=3
    ${event_uuids_list}      Create List
    ${event_ids_list}        Create List
    FOR  ${index}   IN RANGE  0   ${nr_of_event_triggers}
        Commit Event Trigger    ${event_trigger_file}
        Log     EVENT_UUID: ${event_uuid}, EVENT_ID: ${event_id}    console=yes
        Append To List      ${event_uuids_list}      ${event_uuid}
        Append To List      ${event_ids_list}      ${event_id}
    END
    Set Test Variable     ${uuids_list}     ${event_uuids_list}
    Set Test Variable     ${ids_list}       ${event_ids_list}

Delete All Created Event Triggers
    [Arguments]     ${uuids_templates_list}
    FOR     ${el}   IN  @{uuids_templates_list}
        Delete Event Trigger By UUID    ${el}   200
    END

Change Event Trigger Json KeyValue And Save Back To New File
    [Documentation]     Updates Event Trigger file, based on jsonPath argument and value argument.
    [Arguments]     ${jsonPath}      ${valueToUpdate}
    ${file}         Load Json From File     ${VALID EVENT TRIGGER DATA SETS}/create/${event_trigger_file}
    ${json_object}          Update Value To Json	${file}
    ...             ${jsonPath}        ${valueToUpdate}
    ${json_str}     Convert JSON To String    ${json_object}
    ${newTempFile}  Set Variable
    ...             ${VALID EVENT TRIGGER DATA SETS}/create/${temp_file_invalid_aql_in_event_trigger}
    Create File     ${newTempFile}    ${json_str}
    [return]        ${temp_file_invalid_aql_in_event_trigger}
