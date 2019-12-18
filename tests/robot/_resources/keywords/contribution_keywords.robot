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
Documentation    CONTRIBUTION Specific Keywords
Library          XML
Library          String

Resource    ${CURDIR}${/}../suite_settings.robot
Resource    generic_keywords.robot
Resource    template_opt1.4_keywords.robot
Resource    ehr_keywords.robot
Resource    composition_keywords.robot



*** Variables ***
${VALID CONTRI DATA SETS}     ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/contributions/valid
${INVALID CONTRI DATA SETS}   ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/contributions/invalid
${VALID COMPO DATA SETS}     ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/compositions/valid


*** Comments ***
TOC | Table Of Contents

1)  Hight Level Keywords

    Commit CONTRIBUTION
        commit CONTRIBUTION (JSON)
        commit CONTRIBUTION - with preceding_version_uid (JSON)

    Commit Invalid CONRIBUTION

    TODO: finish TOC



*** Keywords ***
# 1) High Level Keywords
commit CONTRIBUTION (JSON)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  COMMIT CONTRIBUTION 1 (JSON)
                        load valid test-data-set    ${valid_test_data_set}
                        POST /ehr/ehr_id/contribution    JSON
                        Set Test Variable    ${body}    ${response.json()}
                        Set Test Variable    ${contribution_uid}    ${body['uid']['value']}
                        Set Test Variable    ${versions}    ${body['versions']}


check response: is positive - returns version id
                        Should Be Equal As Strings    ${response.status_code}    201
                        Set Test Variable    ${body}    ${response.json()}
                        Set Test Variable    ${contribution_uid}    ${body['uid']['value']}
                        Set Test Variable    ${versions}    ${body['versions']}
                        Set Test Variable    ${version_id}    ${body['versions'][0]['id']['value']}
                        Set Test Variable    ${change_type}    ${body['audit']['change_type']['value']}


check content of committed CONTRIBUTION
                        retrieve CONTRIBUTION by contribution_uid (JSON)
                        Length Should Be    ${versions}    1


# TODO: better name --> `commit another CONTRIBUTION` ???
#                       `commit next CONTRIBUTION`    ???
#                       `commit (valid) modification to CONTRIBUTION`
commit CONTRIBUTION - with preceding_version_uid (JSON)
    [Arguments]         ${test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  COMMIT CONTRIBUTION 2 (JSON)
                        inject preceding_version_uid into valid test-data-set    ${test_data_set}
                        POST /ehr/ehr_id/contribution    JSON


check response: is positive - contribution has new version
                        Should Be Equal As Strings    ${response.status_code}    201
                        Set Test Variable    ${body}    ${response.json()}
                        Set Test Variable    ${contribution_uid}    ${body['uid']['value']}
                        Set Test Variable    ${versions}    ${body['versions']}
                        Set Test Variable    ${version_id}    ${body['versions'][0]['id']['value']}
                        Set Test Variable    ${change_type}    ${body['audit']['change_type']['value']}

                        Should Contain  ${version_id}  ::2       
                        Output    ${body}


check change_type of new version is
    [Arguments]         ${type}
    [Documentation]     :change_type: creation, amendment, modification, deleted
                        Should Be Equal As Strings    ${change_type}    ${type}



# VARIATIONS OF COMMITTING INVALID CONTRIBUTIONS
commit invalid CONTRIBUTION (JSON)
    [Arguments]         ${test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  COMMIT CONTRIBUTION 3 (JSON)
                        load invalid test-data-set    ${test_data_set}
                        POST /ehr/ehr_id/contribution    JSON

# commit CONTRIBUTION - no version compo (JSON)
#     [Arguments]         ${test_data_set}
#                         Set Test Variable  ${KEYWORD NAME}  COMMIT CONTRIBUTION 3 (JSON)
#
#                         load invalid test-data-set    ${test_data_set}
#
#                         POST /ehr/ehr_id/contribution    JSON
#
# commit CONTRIBUTION - multiple in/valid version compo (JSON)
#     [Arguments]         ${test_data_set}
#                         Set Test Variable  ${KEYWORD NAME}  COMMIT CONTRIBUTION 5 (JSON)
#
#                         load invalid test-data-set    ${test_data_set}
#
#                         POST /ehr/ehr_id/contribution    JSON
#
#                         Should Be Equal As Strings   ${response.status_code}   400


# VARIATIONS OF RESULTS FROM INVALID CONTRIBUTIONS
check response: is negative indicating errors in committed data
                        Should Be Equal As Strings   ${response.status_code}   400
                        # TODO: keep failing to avoid false positive, rm when has checks.
                        Fail    msg=brake it till you make it!


check response: is negative indicating empty versions list
                        Should Be Equal As Strings   ${response.status_code}   400
                        Set Test Variable    ${body}    ${response.json()}
                        Set Test Variable    ${versions}    ${body['versions']}
                        Length Should Be    ${versions}    0


check response: is negative indicating wrong change_type
                        Should Be Equal As Strings   ${response.status_code}   400
                        Set Test Variable    ${body}    ${response.json()}

                        # TODO: keep failing to avoid false positive
                        #       add checks when available.
                        Fail    msg=brake it till you make it!


check response: is negative indicating non-existent OPT
                        Should Be Equal As Strings   ${response.status_code}   422
                        # TODO: Add checks from response body


commit COMTRIBUTION(S) (JSON)
                        Set Test Variable  ${KEYWORD NAME}  COMMIT COMTRIBUTION(S) (JSON)

            TRACE GITHUB ISSUE  NO-ISSUE-ID  not-ready

# check response: commit COMTRIBUTION(S) (JSON)
#                         Should Be Equal As Strings   ${response.status_code}   201


retrieve CONTRIBUTION by contribution_uid (JSON)
    [Documentation]     DEPENDENCY ${ehr_id} & ${contribution_uid} in test scope
                        Set Test Variable  ${KEYWORD NAME}  GET CONTRI BY CONTRI_UID
                        GET /ehr/ehr_id/contribution/contribution_uid    JSON


check response: is positive - contribution_uid exists
                        Should Be Equal As Strings    ${response.status_code}    200
                        Set Test Variable    ${body}    ${response.json()}
                        Set Test Variable    ${contribution_uid}    ${body['uid']['value']}


check content of retrieved CONTRIBUTION (JSON)
                        check response: is positive - contribution_uid exists


retrieve CONTRIBUTION by fake contri_uid (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET CONTRI BY FAKE CONTRI_UID
                        generate random contribution_uid

            TRACE GITHUB ISSUE  68  not-ready  message=Next step fails due to a bug.

                        GET /ehr/ehr_id/contribution/contribution_uid    JSON


retrieve CONTRIBUTION by fake ehr_id & contri_uid (JSON)
    [Documentation]     Both query params (ehr_id and contribution_uid) are random.
                        Set Test Variable  ${KEYWORD NAME}  GET CONTRI BY FAKE U/IDs
                        generate random ehr_id
                        generate random contribution_uid
                        GET /ehr/ehr_id/contribution/contribution_uid    JSON


retrieve CONTRIBUTION(S) by ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET CONTRI(S) BY EHR_ID
                        GET /ehr/ehr_id/contributions    JSON
                        # NOTE: no such endpoint (anymore)???


retrieve CONTRIBUTION(S) by fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET CONTRI(S) BY EHR_ID

                        generate random ehr_id
                        GET /ehr/ehr_id/contributions    JSON


check response: is negative indicating non-existent ehr_id
                        Should Be Equal As Strings    ${response.status_code}    404
                        Set Test Variable    ${body}    ${response.json()}
                        Should Be Equal As Strings  ${body['error']}  No EHR found with given ID: ${ehr_id}  


check response: is negative indicating non-existent contribution_uid
                        Should Be Equal As Strings    ${response.status_code}    404
                        Set Test Variable    ${body}    ${response.json()}
                        Should Be Equal As Strings  ${body['error']}  No Contribution found with given uid: ${contribution_uid}


check response: is negative indicating non-existent contribution_uid on ehr_id
                        Should Be Equal As Strings    ${response.status_code}    404
                        Set Test Variable    ${body}    ${response.json()}
                        Should Be Equal As Strings  ${body['error']}  TODO: tbd


check response: is positive with list of ${x} contribution(s)
                        Length Should Be    ${versions}    ${x}







# 2) HTTP Methods

# POST

POST /ehr/ehr_id/contribution
    [Arguments]         ${format}
    [Documentation]     DEPENDENCY any keyword that exposes a `${test_data}` variable
    ...                 to test level scope e.g. `load valid test-data-set`

                        # JSON format: defaults apply
                        Run Keyword If      $format=='JSON'    prepare request session
                        ...                 Prefer=return=representation

                        # XML format: overriding defaults
                        Run Keyword If      $format=='XML'    prepare request session
                        ...                 content=application/xml
                        ...                 accept=application/xml
                        ...                 Prefer=return=representation

    ${resp}=            Post Request        ${SUT}   /ehr/${ehr_id}/contribution
                        ...                 data=${test_data}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:    POST /ehr/ehr_id/contribution





# PUT
# DELETE





# GET

GET /ehr/ehr_id/contribution/contribution_uid
    [Arguments]         ${format}
    [Documentation]     DEPENDENCY ${ehr_id} & ${contribution_uid} in test scope

                        Run Keyword If      $format=='JSON'    prepare request session
                        ...                 Prefer=return=representation

                        Run Keyword If      $format=='XML'    prepare request session
                        ...                 content=application/xml
                        ...                 accept=application/xml
                        ...                 Prefer=return=representation

    ${resp}=            Get Request         ${SUT}   /ehr/${ehr_id}/contribution/${contribution_uid}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:    GET /ehr/ehr_id/contribution/contribution_uid


GET /ehr/ehr_id/contributions
    [Arguments]         ${format}
    [Documentation]     DEPENDENCY ${ehr_id} in test scope

                        Run Keyword If      $format=='JSON'    prepare request session
                        ...                 Prefer=return=representation

                        Run Keyword If      $format=='XML'    prepare request session
                        ...                 content=application/xml
                        ...                 accept=application/xml
                        ...                 Prefer=return=representation
                        # NOTE: edpoint does not exist (any more)???
    ${resp}=            Get Request         ${SUT}   /ehr/${ehr_id}/contributions
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:    GET /ehr/ehr_id/contributions





# 3) HTTP Headers
prepare request session
    [Arguments]         ${content}=application/json  ${accept}=application/json  &{others}
    [Documentation]     Prepares request settings for RequestLib
    ...                 :content: application/json (default) / application/xml
    ...                 :accept: application/json (default) / application/xml
    ...                 :others: optional e.g. If-Match={ehrstatus_uid}
    ...                                   e.g. Prefer=return=representation

                        Log Many            ${content}  ${accept}  ${others}

    &{headers}=         Create Dictionary   Content-Type=${content}
                        ...                 Accept=${accept}

                        Run Keyword If      ${others}    Set To Dictionary    ${headers}    &{others}

                        Create Session      ${SUT}    ${${SUT}.URL}
                        ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True

                        Set Test Variable   ${headers}    ${headers}





# 4) FAKE Data
create fake CONTRIBUTION
                        generate random contribution_uid


generate random contribution_uid
    [Documentation]     Generates a random UUIDv4 spec conform `contribution_uid`
    ...                 and exposes it as Test Variable

    ${contri_uid}=      Evaluate    str(uuid.uuid4())    uuid
                        Set Test Variable    ${contribution_uid}    ${contri_uid}





# 5) HELPERS
extract contribution_uid from response (JSON)
        Fail    msg=brake it till you make it!


load valid test-data-set
    [Arguments]        ${valid_test_data_set}

    ${file}=            Get File            ${VALID CONTRI DATA SETS}/${valid_test_data_set}

                        Set Test Variable    ${test_data}    ${file}


load invalid test-data-set
    [Arguments]        ${invalid_test_data_set}

    ${file}=            Get File            ${INVALID CONTRI DATA SETS}/${invalid_test_data_set}

                        Set Test Variable    ${test_data}    ${file}


inject preceding_version_uid into valid test-data-set
    [Arguments]         ${valid_test_data_set}
    ${test_data}=       Load JSON from File    ${VALID CONTRI DATA SETS}/${valid_test_data_set}
    ${test_data}=       Update Value To Json  ${test_data}  $..versions..preceding_version_uid.value
                        ...                   ${version_id}::piri.ehrscape.com::1
                                                        # TODO: rm hardcoded value "piri..."
                        Set Test Variable    ${test_data}    ${test_data}
                        Output    ${test_data}


Output Debug Info:
    [Arguments]         ${KW NAME}
                        ${KEYWORD NAME}=    Set Variable    ${KEYWORD NAME} / ${KW NAME}
                        ${l}=               Evaluate    len('${KEYWORD NAME}')
                        ${line}=               Evaluate    ${l} * '-'
                        Log To Console      \n${line}\n${KEYWORD NAME}\n${line}\n
                        # Log To Console      \trequest headers: \n\t${response.request.headers} \n
                        # Log To Console      \trequest body: \n\t${response.request.body} \n
                        Log To Console      \tresponse status code: \n\t${response.status_code} \n
                        Log To Console      \tresponse headers: \n\t${response.headers} \n
                        # Log To Console      \tresponse body: \n\t${response.content} \n
                        
    ${resti_response}=  Set Variable  ${response.json()}
                        Output    ${resti_response}






# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]
#
# # VARIANTS
#
# # commit valid CONTRIBUTION (POST)
#       201 - created
#       400 - validation error
#       404 - ehr_id does not exist
#
# # commit valid CONTRIBUTION modification
#     - versioning
#     - deleting
#
# # commit invalid CONTRIBUTION
#     - invalid version compo (JSON)
#     - no version compo (JSON)
#     - mix of in/valid version compo (JSON)
#     - ref non-existent OPT
#
# # commit invalid CONTRIBUTION modification
#     - incomplete modification
#     - incorrect modification (e.g. wrong change_type)
