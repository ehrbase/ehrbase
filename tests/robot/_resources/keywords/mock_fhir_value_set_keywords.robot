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
Documentation   MOCK VALUE SET Keywords
Resource        ../suite_settings.robot


*** Variables ***
${VALUE_SET_EXPAND_ENDPOINT}     /fhir/ValueSet/$expand
${VALUE_SET_VALIDATE_CODE_ENDPOINT}     /fhir/ValueSet/idiosyncratic-adverse-reaction-type-1.0.0/$validate-code
${VALUE_SET_ENDPOINT}     /fhir/ValueSet/idiosyncratic-adverse-reaction-type-1.0.0
${VALID_URL_PARAMETER}      http://terminology.hl7.org/ValueSet/v3-HL7StandardVersionCode


*** Keywords ***
GET Create Mock Expectation - ValueSet - Expand Operation
    [Documentation]     Create Mock Expectation for GET ValueSet using expand operation.
    ...     Takes 3 arguments:
    ...     - mockResponse - json file with expected response body
    ...     - invalidParameter - decision on invalid parameter to be sent (emptyUrl,wrongUrl,countMinus,None)
    ...     - statusCode - Example: 200, 400
    [Arguments]     ${mockResponse}=${None}     ${invalidParameter}=${None}     ${statusCode}=200
    IF          '${invalidParameter}' == 'emptyUrl'
        &{params}   Create Dictionary     url=${None}
    ELSE IF     '${invalidParameter}' == 'wrongUrl'
        &{params}   Create Dictionary     url=http://testurl.org
    ELSE IF     '${invalidParameter}' == 'countMinus'
        &{params}   Create Dictionary     url=${VALID_URL_PARAMETER}    count=-2
    ELSE
        &{params}   Create Dictionary     url=${VALID_URL_PARAMETER}
        ...     count=25    includeDesignations=true
        ...     activeOnly=true
    END
    &{req}      Create Mock Request Matcher    GET     ${VALUE_SET_EXPAND_ENDPOINT}   params=&{params}

    IF      ${mockResponse != None}
        ${file}     Get File    ${mockResponse}
        ${fileContentInJSON}    evaluate  json.loads($file)    json
        &{rsp}      Create Mock Response
        ...     status_code=${statusCode}     body=${fileContentInJSON}
    ELSE
        Set Variable    ${rsp}      ${None}
    END
    Create Mock Expectation     ${req}      ${rsp}

GET Create Mock Expectation - ValueSet - Validate Code operation
    [Documentation]     Create Mock Expectation for GET ValueSet using Validate-Code operation.
    ...     Takes 3 arguments:
    ...     - mockResponse - json file with expected response body
    ...     - invalidParameter - decision on invalid parameter to be sent (code, system, empty, None)
    ...     - statusCode - Example: 200, 400, 404...
    [Arguments]     ${mockResponse}=${None}     ${invalidParameter}=${None}     ${statusCode}=200
    IF      '${invalidParameter}' == 'code'
        &{params}   Create Dictionary     system=http://snomed.info/sct   code=111101111
    ELSE IF     '${invalidParameter}' == 'system'
        &{params}   Create Dictionary     system=http://notexistingsystemurl.org   code=281647001
    ELSE IF     '${invalidParameter}' == 'empty'
        &{params}   Create Dictionary     ${None}   ${None}
    ELSE
        &{params}   Create Dictionary     system=http://snomed.info/sct   code=281647001
    END
    &{req}      Create Mock Request Matcher    GET     ${VALUE_SET_VALIDATE_CODE_ENDPOINT}   params=&{params}

    IF      ${mockResponse != None}
        ${file}     Get File    ${mockResponse}
        ${fileContentInJSON}    evaluate  json.loads($file)    json
        &{rsp}      Create Mock Response
        ...     status_code=${statusCode}     body=${fileContentInJSON}
    ELSE
        Set Variable    ${rsp}      ${None}
    END
    Create Mock Expectation     ${req}      ${rsp}

GET Create Mock Expectation - ValueSet - By Id
    [Documentation]     Create Mock Expectation for GET ValueSet using ValueSet Id.
    ...     Takes 3 arguments:
    ...     - mockResponse - json file with expected response body
    ...     - existingId - decision on ValueSet Id. Can be: true, false.
    ...     - statusCode - Example: 200, 404
    [Arguments]     ${mockResponse}=${None}     ${existingId}=true     ${statusCode}=200
    IF      '${existingId}' != 'true'
        &{req}      Create Mock Request Matcher    GET     /fhir/ValueSet/not-existing-value-set-id
    ELSE
        &{req}      Create Mock Request Matcher    GET     ${VALUE_SET_ENDPOINT}
    END
    IF      ${mockResponse != None}
        ${file}     Get File    ${mockResponse}
        ${fileContentInJSON}    evaluate  json.loads($file)    json
        &{rsp}      Create Mock Response
        ...     status_code=${statusCode}     body=${fileContentInJSON}
    ELSE
        Set Variable    ${rsp}      ${None}
    END
    Create Mock Expectation     ${req}      ${rsp}

Send GET - ValueSet - Expand
    [Documentation]     Send request using GET method for ValueSet, with expand operation.
    ...     There are 5 optional parameters:
    ...     - endpoint (define the endpoint to be reached)
    ...     - statusCode (define expected response code)
    ...     - response_headers (define the expected response headers)
    ...     - invalidParameter (emptyUrl/wrongUrl/countMinus - in case negative flow result is expected)
    ...     - response_body (define expected response body)
    [Arguments]  ${endpoint}=${VALUE_SET_EXPAND_ENDPOINT}   ${statusCode}=200  ${response_headers}=${None}
    ...     ${invalidParameter}=${None}     ${response_body}=${None}
    IF          '${invalidParameter}' == 'emptyUrl'
        &{params}   Create Dictionary     url=${None}
    ELSE IF     '${invalidParameter}' == 'wrongUrl'
        &{params}   Create Dictionary     url=http://testurl.org
    ELSE IF     '${invalidParameter}' == 'countMinus'
        &{params}   Create Dictionary     url=${VALID_URL_PARAMETER}    count=-2
    ELSE
        &{params}   Create Dictionary     url=${VALID_URL_PARAMETER}
        ...     count=25     includeDesignations=true   activeOnly=true
    END
    ${resp}     GET On Session
    ...     server
    ...     ${endpoint}
    ...     params=&{params}
    ...     expected_status=anything   headers=${response_headers}
    Status Should Be    ${statusCode}
    IF      ${response_headers != None}
        Verify Response Headers     ${response_headers}     ${resp.headers}
    END
    IF      ${response_body != None}
        Verify Response Body        ${response_body}        ${resp.json()}
    END
    Log To Console      ${resp.json()}
    Set Test Variable   ${response}    ${resp.json()}
    Log To Console      ${response}

Send GET - ValueSet - Validate Code
    [Documentation]     Send request using GET method for ValueSet, with expand operation.
    ...     There are 5 optional parameters:
    ...     - endpoint (define the endpoint to be reached)
    ...     - statusCode (define expected response code)
    ...     - response_headers (define the expected response headers)
    ...     - invalidParameter (emptyUrl/wrongUrl/countMinus - in case negative flow result is expected)
    ...     - response_body (define expected response body)
    [Arguments]  ${endpoint}=${VALUE_SET_EXPAND_ENDPOINT}   ${statusCode}=200  ${response_headers}=${None}
    ...     ${invalidParameter}=${None}     ${response_body}=${None}
    IF      '${invalidParameter}' == 'code'
        &{params}   Create Dictionary     system=http:http://snomed.info/sct   code=111101111
    ELSE IF     '${invalidParameter}' == 'system'
        &{params}   Create Dictionary     system=http://notexistingsystemurl.org   code=281647001
    ELSE IF     '${invalidParameter}' == 'empty'
        &{params}   Create Dictionary     ${None}   ${None}
    ELSE
        &{params}   Create Dictionary     system=http://snomed.info/sct   code=281647001
    END
    ${resp}     GET On Session
    ...     server
    ...     ${endpoint}
    ...     params=&{params}
    ...     expected_status=anything   headers=${response_headers}
    Status Should Be    ${statusCode}
    IF      ${response_headers != None}
        Verify Response Headers     ${response_headers}     ${resp.headers}
    END
    IF      ${response_body != None}
        Verify Response Body        ${response_body}        ${resp.json()}
    END
    Log To Console      ${resp.json()}
    Set Test Variable   ${response}    ${resp.json()}
    Log To Console      ${response}

Send GET - ValueSet - By Id
    [Documentation]     Send request using GET method for ValueSet, using ValueSet Id.
    ...     There are 5 optional parameters:
    ...     - endpoint (define the endpoint to be reached)
    ...     - statusCode (define expected response code)
    ...     - response_headers (define the expected response headers)
    ...     - existingId (true/false)
    ...     - response_body (define expected response body)
    [Arguments]  ${endpoint}=${VALUE_SET_ENDPOINT}   ${statusCode}=200  ${response_headers}=${None}
    ...     ${existingId}=true     ${response_body}=${None}
    &{params}       Create Dictionary     Accept=application/json
    IF      '${existingId}' != 'true'
        ${resp}     GET On Session
        ...     server
        ...     /fhir/ValueSet/not-existing-value-set-id
        ...     expected_status=anything   headers=${response_headers}      params=${params}
    ELSE
        ${resp}     GET On Session
        ...     server
        ...     ${endpoint}
        ...     expected_status=anything   headers=${response_headers}      params=${params}
    END
    Status Should Be    ${statusCode}
    IF      ${response_headers != None}
        Verify Response Headers     ${response_headers}     ${resp.headers}
    END
    IF      ${response_body != None}
        Verify Response Body        ${response_body}        ${resp.json()}
    END
    Log To Console      ${resp.json()}
    Set Test Variable   ${response}    ${resp.json()}
    Log To Console      ${response}

Verify Response Headers
    [Arguments]     ${expected}    ${actual}
    Dictionary Should Contain Sub Dictionary    ${actual}   ${expected}

Verify Response Body
    [Arguments]     ${expected}     ${actual}
    Dictionaries Should Be Equal    ${expected}     ${actual}