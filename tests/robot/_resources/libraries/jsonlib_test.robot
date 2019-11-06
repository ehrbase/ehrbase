# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH).
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
Documentation   Tests for Robot Framework JSON-Compare-Library

Resource    ${CURDIR}${/}../suite_settings.robot
Resource    ${CURDIR}${/}../keywords/generic_keywords.robot

# Suite Setup  startup AQL SUT
# Suite Teardown  shutdown SUT

Force Tags   libtest



*** Variables ***
${ACTUAL JSON PAYLOAD}   ${CURDIR}${/}../fixtures/json_payloads/actual
${EXPECTED JSON PAYLOAD}    ${CURDIR}${/}../fixtures/json_payloads/expected



*** Test Cases ***

TC-001 Payloads as JSON Strings
    ${actual}=          Get File    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Get File    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-strings    ${actual}    ${expected}


TC-002 Payloads as JSON Strings - ignore_string_case
    ${actual}=          Get File    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Get File    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-strings    ${actual}    ${expected}   ignore_string_case=${TRUE}


TC-003 Payloads as Filepaths
    ${actual}=          Set Variable    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Set Variable    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-files    ${actual}    ${expected}


TC-004 Payloads as Filepaths- ignore_order=False
    ${actual}=          Set Variable    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Set Variable    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-files    ${actual}    ${expected}    ignore_order=${FALSE}


TC-005 Payload 1 as String, Payload 2 as Filepath 
    ${actual}=          Get File    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Set Variable    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-string with json-file  ${actual}  ${expected}


TC-006 Payload 1 as String, Payload 2 as Filepath
    ${actual}=          Get File    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Set Variable    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-string with json-file  ${actual}  ${expected}  ignore_order=${FALSE}


TC-007 Payload 1 as Filepath, Payload 2 as String
    ${actual}=          Set Variable    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Get File    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-file with json-string  ${actual}  ${expected}


TC-008 Payload 1 as Filepath, Payload 2 as String
    ${actual}=          Set Variable    ${ACTUAL JSON PAYLOAD}/JSON_payload_A.json
    ${expected}=        Get File    ${EXPECTED JSON PAYLOAD}/JSON_payload_A.json

                        compare json-file with json-string  ${actual}  ${expected}  ignore_order=${FALSE}


TC-009 Payloads Match Exactly
    ${actual}=          Set Variable    {"a": 10, "b": 12, "c": null}
    ${expected}=        Set Variable    {"a": 10, "b": 12, "c": null}

                        payloads match exactly    ${actual}    ${expected}


TC-010 Payloads Match Exactly - ignore_order
    ${actual}=          Set Variable    {"a": 10, "b": 12, "c":[1,2,3]}
    ${expected}=        Set Variable     {"a": 10, "b": 12, "c":[3,2,1]}

                        payloads match exactly    ${actual}    ${expected}    ignore_order=${TRUE}


TC-011 Payloads Match Exactly - ignore_order, ignore_string_case
    ${actual}=          Set Variable    {"a": 10, "b": 12, "c":[1,2,3]}
    ${expected}=        Set Variable     {"c":[3,2,1], "a": 10, "b": 12}

                        payloads match exactly    ${actual}    ${expected}
                        ...                       ignore_order=${TRUE}    ignore_string_case=${TRUE}


TC-012 Payloads Match Exactly - ignore_order
    ${actual}=          Set Variable    {"a": 10, "b": 12, "c":[1,2,3]}
    ${expected}=        Set Variable     {"a": 10, "b": 12, "c":[3,2,1]}

                        Run Keyword And Expect Error  STARTS: JsonCompareError: Payloads do NOT match!
                        ...                           payloads match exactly    ${actual}    ${expected}


TC-013 Actual is Superset of Expected - actual = expected
    ${actual}=          Set Variable    {"a": 10, "b": 12, "c":[3,2,1]}
    ${expected}=        Set Variable     {"a": 10, "b": 12, "c":[3,2,1]}

                        payload is superset of expected    ${actual}    ${expected}


TC-014 Actual is Superset of Expected - actual > expected
    ${actual}=          Set Variable    {"a": 10, "b": 12, "c":[3,2,1], "d": 0}
    ${expected}=        Set Variable     {"a": 10, "b": 12, "c":[3,2,1]}

                        payload is superset of expected    ${actual}    ${expected}


TC-015 Actual is Superset of Expected - ignore_order=False
    ${actual}=          Set Variable    {"a": 10, "b": 12, "c":[3,2,1], "d": 0}
    ${expected}=        Set Variable     {"a": 10, "b": 12, "c":[1,2,3]}

                        Run Keyword And Expect Error    JsonCompareError: Actual payload dosn't meet expectation!
                        ...    payload is superset of expected  ${actual}  ${expected}  ignore_order=${FALSE}


TC-016 Actual is Subset of Expected - actual < expected, ignore_order=False
    [Documentation]     Subset means actual JSON does not meet the expectation, thus should fail. 
    ...                 In other words: actual JSON (response) has less content than expected result data-set.

    ${actual}=          Set Variable    {"a": 10, "b": 12}
    ${expected}=        Set Variable     {"a": 10, "b": 12, "c":[1,2,3]}

                        Run Keyword And Expect Error    JsonCompareError: Actual payload dosn't meet expectation!
                        ...    payload is superset of expected  ${actual}  ${expected}  ignore_order=${FALSE}
