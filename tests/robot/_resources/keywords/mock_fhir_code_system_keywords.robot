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
Documentation   MOCK CODE SYSTEM Keywords
Resource        ../suite_settings.robot

*** Variables ***
${CODE_SYSTEM_LOOKUP_ENDPOINT}     /fhir/CodeSystem/$lookup


*** Keywords ***
GET Create Mock Expectation - CodeSystem - Lookup operation
    [Documentation]     Create Mock Expectation for GET CodeSystem using Lookup operation.
    ...     Takes 1 argument: mockResponse - json file with expected response body.
    [Arguments]     ${mockResponse}=${None}
    &{params}   Create Dictionary     system=http://loinc.org   code=1963-8
    &{req}      Create Mock Request Matcher    GET     ${CODE_SYSTEM_LOOKUP_ENDPOINT}   params=&{params}

    IF      ${mockResponse != None}
        ${file}     Get File    ${mockResponse}
        ${fileContentInJSON}    evaluate  json.loads($file)    json
        &{rsp}      Create Mock Response
        ...     status_code=200     body=${fileContentInJSON}
    ELSE
        Set Variable    ${rsp}      ${None}
    END
    Create Mock Expectation     ${req}      ${rsp}

Send GET Expect Success - CodeSystem - Lookup
    [Documentation]     Send request using GET method for CodeSystem, with Lookup operation.
    ...     There are 4 optional parameters:
    ...     - endpoint (define the endpoint to be reached)
    ...     - response_headers  (define the expected response headers)
    ...     - response_body (define expected response body)
    [Arguments]  ${endpoint}=${CODE_SYSTEM_LOOKUP_ENDPOINT}  ${response_headers}=${None}  ${response_body}=${None}
    &{params}   Create Dictionary     system=http://loinc.org   code=1963-8
    ${resp}     GET On Session
    ...     server
    ...     ${endpoint}
    ...     params=&{params}
    ...     expected_status=anything   headers=${response_headers}
    Status Should Be    200
    IF      ${response_headers != None}
        Verify Response Headers     ${response_headers}     ${resp.headers}
    END
    IF      ${response_body != None}
        Verify Response Body        ${response_body}        ${resp.json()}
    END
    Log To Console      ${resp.json()}
    #${jsonResponseContent}    evaluate  json.loads($resp.content)    json
    Set Test Variable   ${response}    ${resp.json()}
    Log To Console      ${response}

Verify Response Headers
    [Arguments]     ${expected}    ${actual}
    Dictionary Should Contain Sub Dictionary    ${actual}   ${expected}

Verify Response Body
    [Arguments]     ${expected}     ${actual}
    Dictionaries Should Be Equal    ${expected}     ${actual}