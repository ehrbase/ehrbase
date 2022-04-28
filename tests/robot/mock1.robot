*** Settings ***
Library  String
Library  Collections
Library  RequestsLibrary
Library  MockServerLibrary
Library    JSONLibrary
Suite Setup  Create Sessions
Test Teardown  Reset Mock Server


*** Variables ***
${MOCK_URL}     http://localhost:1080
${EHR_ENDPOINT}  /v1/ehr
&{BODY}  var1=value1  var2=value2
${TEST_BODY}    { "_type": "EHR_STATUS",
...     "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
...     "name": {
...     "value": "EHR Status"
...      },
...     "subject": {
...     "external_ref": {
...     "id": {
...     "_type": "GENERIC_ID",
...     "value": "ins01",
...     "scheme": "id_scheme"
...     },
...     "namespace": "examples",
...     "type": "PERSON"
...     }
...     },
...     "is_modifiable": true,
...     "is_queryable": true
...     }
${EHR_EXPECTED_BODY}    {
...     "system_id": {
...     "value": "d60e2348-b083-48ce-93b9-916cef1d3a5a"
...     },
...     "ehr_id": {
...     "value": "7d44b88c-4199-4bad-97dc-d78268e01398"
...     },
...     "ehr_status": {
...     "id": {
...     "_type": "OBJECT_VERSION_ID",
...     "value": "8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1"
...     },
...     "namespace": "local",
...     "type": "EHR_STATUS"
...     },
...     "ehr_access": {
...     "id": {
...     "_type": "OBJECT_VERSION_ID",
...     "value": "59a8d0ac-140e-4feb-b2d6-af99f8e68af8::openEHRSys.example.com::1"
...     },
...     "namespace": "local",
...     "type": "EHR_ACCESS"
...     },
...     "time_created": {
...     "value": "2015-01-20T19:30:22.765+01:00"
...     }
...     }
#&{HEADERS}  Content-type=application/json  Cache-Control=max-age\=3600
&{HEADERS}  Content-Type=application/json
...     Location=${MOCK_URL}/ehr/7d44b88c-4199-4bad-97dc-d78268e01398
...     ETag="7d44b88c-4199-4bad-97dc-d78268e01398"
&{INPUT_HEADERS}  Content-type=application/json  Cache-Control=max-age\=3600  Length=0
${MOCK_REQ}  {"method": "GET", "path": "${EHR_ENDPOINT}"}
${MOCK_RSP}  {"statusCode": 200}
${MOCK_TIMES}  {"remainingTimes": 1, "unlimited": true}
${MOCK_DATA}  {"httpRequest": ${MOCK_REQ}, "httpResponse": ${MOCK_RSP}, "times": ${MOCK_TIMES}}
${VERIFY_DATA}  {"httpRequest": ${MOCK_REQ}, "times": {"atLeast": 1, "atMost": 1}}
${SCHEMA}  { "type" : "object", "required": [ "a"], "properties" : { "a": { "type" : "string"}, "b" : { "type": "number" } } }
${MATCHES_SCHEMA}  { "a": "aaa", "b": 42 }
${BREAKS_SCHEMA}  { "b": 42 }

*** Test Cases ***
Success On Expected POST EHR
    &{req}=  Create Mock Request Matcher  POST  ${EHR_ENDPOINT}
    ${ehrExpectedBody}    evaluate  json.loads($EHR_EXPECTED_BODY)    json
    &{rsp}=  Create Mock Response  status_code=201  headers=&{HEADERS}  body=${ehrExpectedBody}
    Create Mock Expectation  ${req}  ${rsp}
    ${ehrBody}    evaluate  json.loads($TEST_BODY)    json
    Send POST Expect Success  ${EHR_ENDPOINT}   ${ehrBody}
    ${ehrId}    evaluate  json.loads($response.content)    json
    Log     ${ehrId["ehr_id"]["value"]}


*** Keywords ***
Create Sessions
    Create Session  server  ${MOCK_URL}
    Create Mock Session  ${MOCK_URL}

Reset Mock Server
    Dump To Log
    Reset All Requests

Send GET Expect Success
    [Arguments]  ${endpoint}=${ENDPOINT}  ${response_headers}=${None}  ${response_body}=${None}
    ${rsp}=  Get Request  server  ${endpoint}
    Should Be Equal As Strings  ${rsp.status_code}  200
    IF      ${response_headers != None}
        Verify Response Headers  ${response_headers}  ${rsp.headers}
    END
    IF      ${response_body != None}
        Verify Response Body  ${response_body}  ${rsp.json()}
    END

Send GET Expect Failure
    [Arguments]  ${endpoint}=${ENDPOINT}  ${response_code}=404
    ${rsp}=  Get Request  server  ${endpoint}
    Should Be Equal As Strings  ${rsp.status_code}  ${response_code}

Send POST Expect Success
    [Arguments]  ${endpoint}=${ENDPOINT}  ${body}=${BODY}  ${response_code}=201
    Send POST  ${endpoint}  ${body}  ${response_code}  ${headers}=${None}

Send POST Expect Success with Headers
    [Arguments]  ${endpoint}=${ENDPOINT}  ${body}=${BODY}  ${response_code}=201  ${headers}=${None}
    Send POST With Headers  ${endpoint}  ${body}  ${response_code}  headers=${headers}

Send POST Expect Failure
    [Arguments]  ${endpoint}=${ENDPOINT}  ${body}=${BODY}  ${response_code}=404
    Send POST  ${endpoint}  ${body}  ${response_code}

Send POST with Headers
    [Arguments]  ${endpoint}  ${body}  ${response_code}  ${headers}=${None}
    ${body_json}=  Evaluate  json.dumps(${body})  json
    ${rsp}=  Post Request  server  ${endpoint}  headers=${headers}  data=${body_json}
    Should Be Equal As Strings  ${rsp.status_code}  ${response_code}

Send POST
    [Arguments]  ${endpoint}  ${body}  ${response_code}  ${headers}=${None}
    ${body_json}=  Evaluate  json.dumps(${body})  json
    ${rsp}=  Post Request  server  ${endpoint}  data=${body_json}
    Set Test Variable   ${response}     ${rsp}
    Should Be Equal As Strings  ${rsp.status_code}  ${response_code}

Verify Response Headers
    [Arguments]  ${expected}  ${actual}
    Dictionary Should Contain Sub Dictionary  ${actual}  ${expected}

Verify Response Body
    [Arguments]  ${expected}  ${actual}
    Dictionaries Should Be Equal  ${expected}  ${actual}
