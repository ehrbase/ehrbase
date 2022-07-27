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
Documentation    EVENT Trigger Specific Keywords
Resource   ../suite_settings.robot


*** Variables ***
${VALID EVENT TRIGGER DATA SETS}      ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}event_trigger


*** Keywords ***
Generate Event Trigger Id And Update File Content
    [Documentation]     Generate random event id value
    ...     - Update the JSON file with event trigger content.
    ...     - Takes 1 argument, file_content - used to provide event trigger json content.
    ...     - Sets test variable: event_id.
    ...     - Returns json_str with file content, containing generated event id value.
    [Arguments]     ${file_content}
    ${rand_str}     Generate Random String      10       [LOWER]
    ${event_trigger_id_new}     Set Variable        test_trigger_${rand_str}.v0
    ${json_object}      Update Value To Json        json_object=${file_content}
    ...                 json_path=$.id        new_value=${event_trigger_id_new}
    ${json_str}         Convert JSON To String      ${json_object}
            Log     EVENT_ID: ${event_trigger_id_new}       console=yes
            Set Suite Variable   ${event_id}     ${event_trigger_id_new}
    [return]        ${json_str}

Commit Event Trigger
    [Documentation]     Create Event Trigger using JSON file content.
    ...     - ENDPOINT: POST /plugin/event-trigger
    ...     - Takes 1 argument json_event_trigger, to specify the JSON file location.
    ...     - Event trigger `id` is a randomly generated value.
    ...     - DEPENDENCY: `Generate Event Trigger Id And Update File Content`
    ...     - Sets test variables: response, event_uuid
    [Arguments]         ${json_event_trigger}
    ${file}         Load Json From File     ${VALID EVENT TRIGGER DATA SETS}/create/${json_event_trigger}
    ${json_str}     Generate Event Trigger Id And Update File Content       ${file}
    &{headers}      Create Dictionary   Content-Type=application/json
                    ...             Accept=application/json
    Check If Session With Plugin Endpoint Exists
    ${resp}         POST On Session     eventtriggersut
    ...             /event-trigger      expected_status=anything
    ...             data=${json_str}    headers=${headers}
                    #Log To Console      ${resp.content}
                    Should Be Equal As Strings          ${resp.status_code}     200
                    Set Suite Variable   ${response}     ${resp}
                    Set Suite Variable   ${event_uuid}   ${resp.content}

Get Event Trigger By Criteria
    [Documentation]     Get Event Trigger using specific criteria.
    ...     - First argument `criteria` can be: event id or event uuid.
    ...     - Second argument (optional), `expected_code` can be 200 or 404
    ...     - ENDPOINT: GET /plugin/event-trigger/{criteria}
    [Arguments]     ${criteria}     ${expected_code}=200
    Check If Session With Plugin Endpoint Exists
    ${resp}         GET On Session      eventtriggersut      /event-trigger/${criteria}      expected_status=anything
                    #Log To Console      ${resp.content}
                    Should Be Equal As Strings       ${resp.status_code}     ${expected_code}
                    Set Suite Variable   ${response}     ${resp}
                    Return From Keyword If      '${resp.status_code}' != '200'
                    Set Suite Variable   ${response_event_trigger}   ${resp.content}

Delete Event Trigger By UUID
    [Documentation]     - Delete Event Trigger using it's UUID.
    ...                 - UUID value is provided using `uuid_val` argument.
    ...                 - expected_code value is optional (default is 200).
    [Arguments]     ${uuid_val}     ${expected_code}=200
    Check If Session With Plugin Endpoint Exists
    ${resp}         DELETE On Session   eventtriggersut      /event-trigger/${uuid_val}      expected_status=anything
                    Should Be Equal As Strings      ${resp.status_code}     ${expected_code}
                    Set Test Variable   ${response}     ${resp}


Get All Event Triggers
    [Documentation]     Get all event triggers.
    ...     - Expects status code to be 200.
    ...     - Sets 2 test variables: response (code) and all_event_triggers
    ...     - ENDPOINT: GET /plugin/event-trigger
    Check If Session With Plugin Endpoint Exists
    ${resp}         GET On Session      eventtriggersut      /event-trigger
                    #Log To Console      ${resp.content}
                    Should Be Equal As Strings      ${resp.status_code}     200
                    Set Test Variable   ${response}             ${resp}
                    Set Test Variable   ${all_event_triggers}   ${resp.json()}
                    ${event_triggers_len}    Get Length      ${all_event_triggers}
                    Should Be True      ${event_triggers_len}>2

Check If Session With Plugin Endpoint Exists
    [Documentation]     Checks if SUT session with /plugin endoint exists.
    ...     If it does not exist, it will create the session.
    ...     - Call this keyword before sending POST, GET requests.
    ${sessionExists}     Session Exists      eventtriggersut
    IF      ${sessionExists} == ${FALSE}
        Create Session      eventtriggersut    ${PLUGINURL}
        ...     debug=2     auth=${CREDENTIALS}     verify=True
    END

