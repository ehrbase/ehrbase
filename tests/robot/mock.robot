*** Settings ***
Library  String
Library  Collections
Library  RequestsLibrary
Library  MockServerLibrary
Library    JSONLibrary
Suite Setup  Create Sessions
Test Teardown  Reset Mock Server


*** Variables ***
${MOCK_URL}     http://localhost:8080/ehrbase/rest/openehr/v1
${ENDPOINT}  /definition/template/adl1.4/family_history/example
&{BODY}  var1=value1  var2=value2
&{HEADERS}  Accept=application/json  Content-Type=application/xml  Prefer=return=representation
&{INPUT_HEADERS}  Content-type=application/json  Cache-Control=max-age\=3600  Length=0
${MOCK_REQ}  {"method": "GET", "path": "${ENDPOINT}"}
${MOCK_RSP}  {"statusCode": 200}
${MOCK_TIMES}  {"remainingTimes": 1, "unlimited": true}
${MOCK_DATA}  {"httpRequest": ${MOCK_REQ}, "httpResponse": ${MOCK_RSP}, "times": ${MOCK_TIMES}}
${VERIFY_DATA}  {"httpRequest": ${MOCK_REQ}, "times": {"atLeast": 1, "atMost": 1}}
${SCHEMA}  { "type" : "object", "required": [ "a"], "properties" : { "a": { "type" : "string"}, "b" : { "type": "number" } } }
${MATCHES_SCHEMA}  { "a": "aaa", "b": 42 }
${BREAKS_SCHEMA}  { "b": 42 }

*** Test Cases ***
Success On Expected GET
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Send GET Expect Success  ${ENDPOINT}

Success On Expected GET With Specified Data
    Create Mock Expectation With Data  ${MOCK_DATA}
    Send GET Expect Success  ${ENDPOINT}

Failure On GET With Mismatched Method
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Send GET Expect Failure  endpoint=${ENDPOINT}

Failure On GET With Mismatched Endpoint
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Send GET Expect Failure  endpoint=/mismatched

Success On Expected GET With Response Body
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200  headers=${HEADERS}  body=${BODY}
    Create Mock Expectation  ${req}  ${rsp}
    Send GET Expect Success  ${ENDPOINT}  response_headers=${HEADERS}  response_body=${BODY}

Success On Two Expected GETs
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}  count=2
    Repeat Keyword  2  Send GET Expect Success  ${ENDPOINT}

Success On Expected POST With Body
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}  body=${BODY}
    &{rsp}=  Create Mock Response  status_code=201
    Create Mock Expectation  ${req}  ${rsp}
    Send POST Expect Success  ${ENDPOINT}  ${BODY}

Failure On POST With Mismatched Body
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}  body=${BODY}
    &{rsp}=  Create Mock Response  status_code=201
    Create Mock Expectation  ${req}  ${rsp}
    &{mismatched}=  Create Dictionary  var1=mismatch  var2=value2
    Send POST Expect Failure  ${ENDPOINT}  ${mismatched}

Success On Expected POST With Json Schema
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}  body_type=JSON_SCHEMA  body=${SCHEMA}
    &{rsp}=  Create Mock Response  status_code=201
    Create Mock Expectation  ${req}  ${rsp}
    Send POST Expect Success  ${ENDPOINT}  ${MATCHES_SCHEMA}

Failure On POST With Mismatched Schema
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}  body_type=JSON_SCHEMA  body=${SCHEMA}
    &{rsp}=  Create Mock Response  status_code=201
    Create Mock Expectation  ${req}  ${rsp}
    Send POST Expect Failure  ${ENDPOINT}  ${BREAKS_SCHEMA}

Success On Verify
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Send GET Expect Success  ${ENDPOINT}
    Verify Mock Expectation  ${req}

Success On Verify With Two Expected GETs
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}  count=2
    Repeat Keyword  2  Send GET Expect Success  ${ENDPOINT}
    Verify Mock Expectation  ${req}  count=2

Success On Verify With Specified Data
    Create Mock Expectation With Data  ${MOCK_DATA}
    Send GET Expect Success  ${ENDPOINT}
    Verify Mock Expectation With Data  ${VERIFY_DATA}

Success On Verify With Unspecified Number Of GETs
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Repeat Keyword  3  Send GET Expect Success  ${ENDPOINT}
    Verify Mock Expectation  ${req}  exact=${false}

Success On Verify POST With Body
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}  body=${BODY}
    &{rsp}=  Create Mock Response  status_code=201
    Create Mock Expectation  ${req}  ${rsp}
    Send POST Expect Success  ${ENDPOINT}  ${BODY}
    Verify Mock Expectation  ${req}

Success On Verify POST With Headers
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}  headers=${HEADERS}
    &{rsp}=  Create Mock Response  status_code=201
    Create Mock Expectation  ${req}  ${rsp}
    #Send POST Expect Success  ${ENDPOINT}  ${BODY}  headers=${INPUT_HEADERS}
    Send POST Expect Success with Headers  ${ENDPOINT}  ${BODY}  headers=&{INPUT_HEADERS}
    Verify Mock Expectation  ${req}

Failure On Verify With Missing GET
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Run Keyword And Expect Error  *  Verify Mock Expectation  ${req}

Failure On Verify With Mismatched Endpoint
    &{req}=  Create Mock Request Matcher  GET  /mismatched
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Send GET Expect Success  endpoint=/mismatched
    &{req_mis}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    Run Keyword And Expect Error  *  Verify Mock Expectation  ${ENDPOINT}

Failure On Verify With Too Many GETs
    &{req}=  Create Mock Request Matcher  GET  ${ENDPOINT}
    &{rsp}=  Create Mock Response  status_code=200
    Create Mock Expectation  ${req}  ${rsp}
    Repeat Keyword  2  Send GET Expect Success  ${ENDPOINT}
    Run Keyword And Expect Error  *  Verify Mock Expectation  ${req}  count=1

Failure On Verify POST With Mismatched Body
    &{req}=  Create Mock Request Matcher  POST  ${ENDPOINT}  body=${BODY}
    &{rsp}=  Create Mock Response  status_code=201
    Create Mock Expectation  ${req}  ${rsp}
    Send POST Expect Success  ${ENDPOINT}  ${BODY}
    &{mismatched}=  Create Dictionary  var1=mismatch  var2=value2
    &{req_mis}=  Create Mock Request Matcher  POST  ${ENDPOINT}  body=${mismatched}
    Run Keyword And Expect Error  *  Verify Mock Expectation  ${req_mis}

Success On Request Sequence
    &{req1}=  Create Mock Request Matcher  GET  /endpoint1
    &{req2}=  Create Mock Request Matcher  POST  /endpoint2
    &{req3}=  Create Mock Request Matcher  GET  /endpoint3
    &{rsp}=  Create Mock Response  status_code=200
    &{rsp2}=  Create Mock Response  status_code=201

    Create Mock Expectation  ${req1}  ${rsp}
    Create Mock Expectation  ${req2}  ${rsp2}
    Create Mock Expectation  ${req3}  ${rsp}

    Send GET Expect Success  /endpoint1
    Send POST Expect Success  /endpoint2
    Send GET Expect Success  /endpoint3

    @{seq}=  Create List  ${req1}  ${req2}  ${req3}
    Verify Mock Sequence  ${seq}

Failure On Partial Request Sequence
    &{req1}=  Create Mock Request Matcher  GET  /endpoint1
    &{req2}=  Create Mock Request Matcher  POST  /endpoint2
    &{req3}=  Create Mock Request Matcher  GET  /endpoint3
    &{rsp}=  Create Mock Response  status_code=200
    &{rsp2}=  Create Mock Response  status_code=201

    Create Mock Expectation  ${req1}  ${rsp}
    Create Mock Expectation  ${req2}  ${rsp2}
    Create Mock Expectation  ${req3}  ${rsp}

    Send GET Expect Success  /endpoint1
    Send POST Expect Success  /endpoint2

    @{seq}=  Create List  ${req1}  ${req2}  ${req3}
    Run Keyword And Expect Error  *  Verify Mock Sequence  ${seq}

Failure On Misordered Request Sequence
    &{req1}=  Create Mock Request Matcher  GET  /endpoint1
    &{req2}=  Create Mock Request Matcher  POST  /endpoint2
    &{req3}=  Create Mock Request Matcher  GET  /endpoint3
    &{rsp}=  Create Mock Response  status_code=200
    &{rsp2}=  Create Mock Response  status_code=201

    Create Mock Expectation  ${req1}  ${rsp}
    Create Mock Expectation  ${req2}  ${rsp2}
    Create Mock Expectation  ${req3}  ${rsp}

    Send POST Expect Success  /endpoint2
    Send GET Expect Success  /endpoint1
    Send GET Expect Success  /endpoint3

    @{seq}=  Create List  ${req1}  ${req2}  ${req3}
    Run Keyword And Expect Error  *  Verify Mock Sequence  ${seq}

Success On Default GET Expectation
    Create Default Mock Expectation  GET  ${ENDPOINT}
    Send GET Expect Success  ${ENDPOINT}

Success On Default POST Expectation
    Create Default Mock Expectation  POST  ${ENDPOINT}  response_code=201
    Send POST Expect Success  ${ENDPOINT}  response_code=201

Success On Default GET Expectation With Response Body
    Create Default Mock Expectation  GET  ${ENDPOINT}  response_headers=${HEADERS}  response_body=${BODY}
    Send GET Expect Success  ${ENDPOINT}  response_headers=${HEADERS}  response_body=${BODY}

Success On Retrieve Requests
    Create Default Mock Expectation  GET  ${ENDPOINT}
    Create Default Mock Expectation  GET  /endpoint2
    Send GET Expect Success  ${ENDPOINT}
    Send GET Expect Success  /endpoint2
    ${rsp}=  Retrieve Requests  ${ENDPOINT}
    Log  ${rsp.text}  DEBUG
    ${rsp_str}=  Convert To String  ${rsp.text}
    Should Contain  ${rsp_str}  GET
    Should Contain  ${rsp_str}  ${ENDPOINT}
    Should Not Contain  ${rsp_str}  /endpoint2

Success On Clear Requests
    Create Default Mock Expectation  GET  ${ENDPOINT}
    Send GET Expect Success  ${ENDPOINT}
    Clear Requests  ${ENDPOINT}
    ${rsp}=  Retrieve Requests  ${ENDPOINT}
    Log  ${rsp.text}  DEBUG
    ${rsp_str}=  Convert To String  ${rsp.text}
    Should Not Contain  ${rsp_str}  GET
    Should Not Contain  ${rsp_str}  ${ENDPOINT}

Success On Retrieve Expectations
    Create Default Mock Expectation  GET  ${ENDPOINT}
    ${rsp}=  Retrieve Expectations  ${ENDPOINT}
    Log  ${rsp.text}  DEBUG
    ${rsp_str}=  Convert To String  ${rsp.text}
    Should Contain  ${rsp_str}  GET
    Should Contain  ${rsp_str}  ${ENDPOINT}

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
    Run Keyword If   ${response_headers != None}  Verify Response Headers  ${response_headers}  ${rsp.headers}
    Run Keyword If   ${response_body != None}  Verify Response Body  ${response_body}  ${rsp.json()}

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
    Should Be Equal As Strings  ${rsp.status_code}  ${response_code}

Verify Response Headers
    [Arguments]  ${expected}  ${actual}
    Dictionary Should Contain Sub Dictionary  ${actual}  ${expected}

Verify Response Body
    [Arguments]  ${expected}  ${actual}
    Dictionaries Should Be Equal  ${expected}  ${actual}