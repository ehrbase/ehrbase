# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
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
Metadata    Version    0.1.0
Metadata    Author    *Pablo Pazos*
Metadata    Created    2019.12.02

Documentation   D) Main flow: Create new EHR with other_details
...
...            https://github.com/ehrbase/project_management/issues/55
...

Library    Collections
Library    OperatingSystem

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

# Suite Setup    startup SUT
# Suite Teardown    shutdown SUT
# Test Template    client sends POST request to /ehr

Force Tags    create_ehr

*** Test Cases ***
Create EHR with other_details (JSON)

    #start request session    JSON
    Set Log Level    TRACE

    generate random subject_id

    ${json_ehr_status}=        Load JSON From File   ${VALID EHR DATA SETS}/000_ehr_status_with_other_details.json
    ${json_ehr_status}=        Update Value To Json  ${json_ehr_status}   $.subject.external_ref.id.value   ${subject_id}                                                                                       # alternative syntax
                        #create new EHR with ehr_status    ${json_ehr}

                        Create Session        ${SUT}    ${${SUT}.URL}
                        ...                   auth=${${SUT}.CREDENTIALS}    debug=2    verify=True

    &{headers}=         Create Dictionary     Content-Type=application/json
                        ...                   Accept=application/json
                        ...                   Prefer=return=representation

    ${resp}=            Post Request          ${SUT}    /ehr
                        ...                   data=${json_ehr_status}
                        ...                   headers=${headers}

                        Log To Console        ${json_ehr_status}
                        Log To Console        ${resp.json()['ehr_status']}

                        Set Test Variable   ${actual_ehr_status}    ${resp.json()['ehr_status']}
                        # Set Test Variable   ${response body}    ${resp.content}

                        Should Be Equal As Strings 	${resp.status_code} 	201

    # this converts dict to json string, without strings the compare jsons keyword doesn't work
    ${actual_ehr_status}=    evaluate    json.dumps(${actual_ehr_status})    json
    ${json_ehr_status_string}=    evaluate    json.dumps(${json_ehr_status})    json
    &{diff}=            compare jsons    ${actual_ehr_status}    ${json_ehr_status_string}

                        Log To Console    ${diff}

                        #compare_json_payloads  ${ehr_status_json_string}  ${json_ehr_status}
                        Should Be Empty  ${diff}  msg=DIFF DETECTED!
#######


    #&{resp}=            REST.POST    ${baseurl}/ehr    ${ehr_status_object}
                        # Integer      response status    201  200

                        #Set Test Variable    ${response}    ${resp}

                        #Output Debug Info To Console  # NOTE: won't work with content-type=XML

    #create new EHR with other_details for subject_id (JSON)    ${subject_id}

    # defined by previous keyword
    #Log To Console    ${response}


    #verify response


*** Keywords ***
verify response
    Should Be Equal As Strings    ${response.status}    201
    #${json_response}=  Set Variable  ${response.json()}
    ${json_ehr_expected}=    Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec_with_other_details.json
    ${json_ehr_expected}=    Update Value To Json  ${json_ehr_expected}   $.subject.external_ref.id.value   ${subject_id}
    Log To Console    ${response.body}
    #${response_string}=   Convert To String    ${response.body}
    #Log To Console    ${response_string}
    #&{response_string}=    Evaluate    json.loads('''${response.body}''')    json
    #Log To Console    ${response_string}

    &{diff}=            compare jsons    ${response_string}    ${json_ehr_expected}
    Should Be Empty  ${diff}  msg=DIFF DETECTED!
