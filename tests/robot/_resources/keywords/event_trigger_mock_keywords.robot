# Copyright (c) 2022 Vladislav Ploaia.
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
Documentation   EVENT TRIGGER MOCK KEYWORDS
Resource        ../suite_settings.robot

*** Variables ***
${TEST_PATH_EVENT_TRIGGER_ENDPOINT}     /path

*** Keywords ***
POST Create Mock Expectation Event Trigger
    [Arguments]     ${requestMockFile}=${None}     ${responseMockFile}=${None}     ${statusCode}=200
    #${requestFile}     Get File    ${requestMockFile}
    #${requestFileContentInJSON}    evaluate  json.loads($requestFile)    json
    ${requestFileContentInJSON}     Load Json From File     ${requestMockFile}
    &{req}      Create Mock Request Matcher    POST
    ...     ${TEST_PATH_EVENT_TRIGGER_ENDPOINT}     body=${requestFileContentInJSON}
    ######
    ${responseFileContentInJSON}     Load Json From File     ${responseMockFile}
    &{rsp}      Create Mock Response
    ...     status_code=${statusCode}     #body=${responseFileContentInJSON}
    Create Mock Expectation     ${req}      ${rsp}

Send POST Endpoint Expect Success
    [Arguments]  ${endpoint}=${TEST_PATH_EVENT_TRIGGER_ENDPOINT}
    ...     ${body_content}=${None}
    IF      ${body_content != None}
        ${file}     Get File    ${body_content}
        ${fileContentInJSON}    evaluate  json.loads($file)    json
    END
    &{headers}      Create Dictionary       Accept=application/json     Content-Type=application/json

    ${resp}     POST On Session
    ...     server
    ...     ${endpoint}
    ...     data=${fileContentInJSON}
    ...     headers=${headers}
    ...     expected_status=anything
    Should Be Equal As Strings      ${resp.status_code}      200
    Log To Console      ${resp.json()}
    Set Test Variable   ${response}    ${resp.json()}
    Log To Console      ${response}

Verify Response Body
    [Arguments]     ${expected}     ${actual}
    Dictionaries Should Be Equal    ${expected}     ${actual}
