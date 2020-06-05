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
Documentation   EHR Keywords
Resource        generic_keywords.robot
Library         XML
Library         REST
Library         Collections
Library         distutils.util



*** Variables ***
${VALID EHR DATA SETS}       ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/ehr/valid
${INVALID EHR DATA SETS}     ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/ehr/invalid


*** Keywords ***
# 1) High Level Keywords

update EHR: set ehr_status is_queryable
    [Arguments]         ${value}
    [Documentation]     valid values: ${TRUE}, ${FALSE}
    ...                 default: ${TRUE}

    # from preceding request of `create new EHR ...`
    extract ehr_id from response (JSON)
    extract system_id from response (JSON)
    extract subject_id from response (JSON)
    extract ehrstatus_uid (JSON)
    extract ehr_status from response (JSON)

    set is_queryable / is_modifiable    is_queryable=${value}

    set ehr_status of EHR


update EHR: set ehr-status modifiable
    [Arguments]         ${value}
    [Documentation]     valid values: ${TRUE}, ${FALSE}
    ...                 default: ${TRUE}

    # from preceding request of `create new EHR ...`
    extract ehr_id from response (JSON)
    extract system_id from response (JSON)
    extract subject_id from response (JSON)
    extract ehrstatus_uid (JSON)
    extract ehr_status from response (JSON)

    set is_queryable / is_modifiable    is_modifiable=${value}

    set ehr_status of EHR


check response of 'update EHR' (JSON)
                        Integer     response status    200
                        String    response body uid value    ${ehrstatus_uid[0:-1]}2

                        # TODO: @WLAD check Github Issue #272
                        # String    response body subject external_ref id value    ${subject_Id}

                        String    response body _type    EHR_STATUS


# 2) HTTP Methods
#    POST
#    PUT
#    GET
#    DELETE
#
# 3) HTTP Headers
#
# 4) FAKE Data



create new EHR
    [Documentation]     Creates new EHR record with a server-generated ehr_id.
    ...                 DEPENDENCY: `prepare new request session`

    &{resp}=            REST.POST    ${baseurl}/ehr
                        Integer      response status    201

                        extract ehr_id from response (JSON)
                        extract system_id from response (JSON)

                        # TODO: @WLAD check Github Issue #272
                        # extract subject_id from response (JSON)

                        extract ehr_status from response (JSON)
                        extract ehrstatus_uid (JSON)

                        Set Suite Variable    ${response}    ${resp}

                        Output Debug Info To Console  # NOTE: won't work with content-type=XML


#TODO: @WLAD  rename KW name when refactor this resource file
create supernew ehr
    [Documentation]     Creates new EHR record with a server-generated ehr_id.
    ...                 DEPENDENCY: `prepare new request session`

    &{resp}=            REST.POST    ${baseurl}/ehr
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


create new EHR (XML)
    [Documentation]     Creates new EHR record with a server-generated ehr_id.
    ...                 DEPENDENCY: `prepare new request session`

    &{resp}=            REST.POST    ${baseurl}/ehr
                        Should Be Equal As Strings    ${resp.status}    201

                        Set Test Variable    ${response}    ${resp}

                        extract ehr_id from response (XML)
                        extract system_id from response (XML)
                        extract ehrstatus_uid (XML)

                        Output Debug Info To Console







# ooooooooo.   oooooooooooo  .oooooo..o ooooooooo.     .oooooo.   ooooo      ooo  .oooooo..o oooooooooooo  .oooooo..o
# `888   `Y88. `888'     `8 d8P'    `Y8 `888   `Y88.  d8P'  `Y8b  `888b.     `8' d8P'    `Y8 `888'     `8 d8P'    `Y8
#  888   .d88'  888         Y88bo.       888   .d88' 888      888  8 `88b.    8  Y88bo.       888         Y88bo.
#  888ooo88P'   888oooo8     `"Y8888o.   888ooo88P'  888      888  8   `88b.  8   `"Y8888o.   888oooo8     `"Y8888o.
#  888`88b.     888    "         `"Y88b  888         888      888  8     `88b.8       `"Y88b  888    "         `"Y88b
#  888  `88b.   888       o oo     .d8P  888         `88b    d88'  8       `888  oo     .d8P  888       o oo     .d8P
# o888o  o888o o888ooooood8 8""88888P'  o888o         `Y8bood8P'  o8o        `8  8""88888P'  o888ooooood8 8""88888P'
#
# [ RESPONSE VALIDATION ]

# POST POST POST POST
#/////////////////////

validate POST response - 201 created ehr
    [Documentation]     CASE: new ehr was created.
    ...                 Request was send with `Prefer=return=representation`.

    Integer             response status    201
    Object              response body


validate POST response - 204 no content
    [Documentation]     CASE: new ehr was created.
    ...                 Request was send w/o `Prefer=return` header or with
    ...                 `Prefer=return=minimal`. Body has to be empty.

    Integer             response status    204
    String              response body    ${EMPTY}


# PUT PUT PUT PUT PUT
#/////////////////////

validate PUT response - 204 no content
    [Documentation]     CASE: new ehr was created w/ given ehr_id.
    ...                 Request was send w/o `Prefer=return` header or with
    ...                 `Prefer=return=minimal`. Body has to be empty.

    Integer             response status    204
    String              response body    ${EMPTY}





# create EHR XML
#     [Documentation]     Creates new EHR record and extracts server generated ehr_id
#     ...                 Puts `ehr_id` on Test Level scope so that it can be accessed
#     ...                 by other keywords, e.g. `commit composition (XML)`.

#     Log         DEPRECATION WARNING: @WLAD remove this KW - it's only used in old AQL-QUERY tests.
#                 ...         level=WARN

#     &{headers}=         Create Dictionary  Prefer=return=representation  Accept=application/xml
#     ${resp}=            Post Request       ${SUT}     /ehr    headers=${headers}
#                         Should Be Equal As Strings    ${resp.status_code}    201

#     ${xresp}=           Parse Xml          ${resp.text}
#                         Log Element        ${xresp}
#                         Set Test Variable  ${xresp}   ${xresp}

#     ${xehr_id}=         Get Element        ${xresp}    ehr_id/value
#                         Set Test Variable  ${ehr_id}   ${xehr_id.text}
#                         # Log To Console     ${ehr_id}


create new EHR with ehr_status
    [Arguments]         ${ehr_status_object}
    [Documentation]     Creates new EHR record with a server-generated ehr_id.
    ...                 DEPENDENCY: `prepare new request session`
    ...                 :ehr_status_object: ehr_status_as_json_string_or_file

    &{resp}=            REST.POST    ${baseurl}/ehr    ${ehr_status_object}
                        # Integer      response status    201  200

                        Set Suite Variable    ${response}    ${resp}

                        Output Debug Info To Console  # NOTE: won't work with content-type=XML


create new EHR by ID
    [Arguments]         ${ehr_id}
    [Documentation]     Create a new EHR with the specified EHR identifier.
    ...                 DEPENDENCY: `prepare new request session`

    &{resp}=            REST.PUT    ${baseurl}/ehr/${ehr_id}

                        Set Test Variable    ${response}    ${resp}

                        Output Debug Info To Console  # NOTE: won't work with content-type=XML


create new EHR for subject_id (JSON)
    [Arguments]         ${subject_id}
    ${json_ehr}=        Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec.json
    ${json_ehr}=        Update Value To Json  ${json_ehr}   $.subject.external_ref.id.value   ${subject_id}

    # same as the line above / # alternative syntax
    # ${json_ehr}=        Update Value To Json  ${json_ehr}   $['subject']['external_ref']['id']['value']   ${subject_id}

                        create new EHR with ehr_status    ${json_ehr}


create new EHR with other_details for subject_id (JSON)
    [Arguments]         ${subject_id}
    ${json_ehr}=        Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec_with_other_details.json
    ${json_ehr}=        Update Value To Json  ${json_ehr}   $.subject.external_ref.id.value   ${subject_id}                                                                                       # alternative syntax
                        create new EHR with ehr_status    ${json_ehr}


create new EHR with subject_id (JSON)

                        generate random subject_id

    ${json_ehr}=        Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec.json
    ${json_ehr}=        Update Value To Json  ${json_ehr}   $.subject.external_ref.id.value   ${subject_id}

                        create new EHR with ehr_status    ${json_ehr}

                        extract ehr_id from response (JSON)
                        extract system_id from response (JSON)
                        extract ehr_status from response (JSON)


check content of created EHR (JSON)
                        Integer      response status    201

                        String    response body ehr_id value                    ${ehr_id}
                        String    response body system_id value                 ${system_id}

                        # TODO: @WLAD check Github issue #272
                        # String    response body ehr_status subject external_ref id value    ${subject_Id}

                        Object    response body ehr_status                      ${ehr_status}

                        # extract ehr_id from response (JSON)
                        # extract system_id from response (JSON)
                        # extract subject_id from response (JSON)    # is in ehr_status
                        # extract ehr_status from response (JSON)


# create new EHR for subject_id (XML)
#     # generate random subject_id
#     [Arguments]         ${subject_id}
#     ${json_ehr}=        Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec.json
#     ${json_ehr}=        Update Value To Json  ${json_ehr}   $['subject'].['id']['value']   ${subject_id}
#     &{headers}=         Create Dictionary     Content-Type=application/json  Prefer=return=representation
#
#     ${resp}=            Post Request          ${SUT}   /ehr   headers=${headers}   data=${json_ehr}
#                         Log To Console    ${resp.request.body}
#                         Log To Console    ${resp.content}
#                         Should Be Equal As Strings   ${resp.status_code}   201


retrieve EHR by ehr_id
    [Documentation]     Retrieves EHR with specified ehr_id.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_id` e.g.
    ...                             - `create new EHR`

    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}

                        Output Debug Info To Console

                        Integer     response status         200


retrieve EHR by subject_id
    [Documentation]     Retrieves EHR with specified subject_id and namespace.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `subject_id` e.g.
    ...                             - `create new EHR`
    ...                             - `generate random subject_id`

    &{resp}=            REST.GET    ${baseurl}/ehr?subject_id=${subject_id}&subject_namespace=patients
                        Output     response
                        Integer    response status    200


check content of retrieved EHR (JSON)

    Integer     response status         200

    # NOTE: RESTInstace provides a nice way to verify results

    #         |<---    actual data                 --->|<--- expected data --->|
    String    response body ehr_id value                    ${ehr_id}
    String    response body system_id value                 ${system_id}

    # TODO: @Wlad check Github Issue #272
    # String    response body ehr_status subject external_ref id value    ${subject_Id}

    Object    response body ehr_status                      ${ehr_status}
    # Boolean   response body ehr_status is_queryable         ${TRUE}           # is already checked
    # Boolean   response body ehr_status is_modifiable        ${TRUE}           # in ehr_status


    # It's not required to put actuals into variables and apply verification keywords
    # --- actual data ---|                                     # --- expected data ---|
    ${actual_ehrid}=      String  response body ehr_id value   ${ehr_id}
    ${actual_ehrstatus}=  Object  response body ehr_status     ${ehr_status}

                        # Output    ${actual_ehrid}[0]
                        # Output    ${ehr_id}
                        # Output    ${actual_ehrstatus}[0]
                        # Output    ${ehr_status}

                        # Should Be Equal    ${actual_ehrid}[0]    ${ehr_id}
                        # Should Be Equal    ${actual}[0]    ${ehr_status}

    # NOTE: this checks are not required any more
    # Should Be Equal As Strings   ${ehr_id}   ${resp.json()['ehr_id']['value']}
    # Should Be Equal As Strings   ${is_queryable}   ${resp.json()['ehr_status']['is_queryable']}
    # Should Be Equal As Strings   ${is_modifiable}   ${resp.json()['ehr_status']['is_modifiable']}
    # Should Be Equal As Strings   ${subject_id}   ${resp.json()['ehr_status']['subject']['external_ref']['id']['value']}




retrieve non-existing EHR by ehr_id
    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}
                        Output

                        Integer     response status    404


retrieve non-existing EHR by subject_id
    &{resp}=            REST.GET    ${baseurl}/ehr?subject_id=${subject_id}&subject_namespace=patients
                        Output     response

                        Integer    response status    404


get ehr_status of EHR
    [Documentation]     Gets status of EHR with given ehr_id.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_id` e.g.
    ...                             - `create new EHR`
    ...                             - `generate random ehr_id`

    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/ehr_status
                        ...         headers={"Content-Type": "application/json"}
                        # ...         headers={"If-Match": null}
                        Set Test Variable    ${response}    ${resp}

                        # Output Debug Info To Console


# get ehr_status of EHR with version at time
#     [Arguments]         ${version_at_time}
#     [Documentation]     Gets status of EHR with given `ehr_id` and `version at time`.
#     ...                 DEPENDENCY: `prepare new request session` and keywords that
#     ...                             create and expose an `ehr_id` e.g.
#     ...                             - `create new EHR`
#     ...                             - `generate random ehr_id`
#
#     &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/ehr_status?version_at_time=${version_at_time}
#                         Set Test Variable    ${response}    ${resp}
#
#                         # Output Debug Info To Console


get ehr_status of fake EHR
    [Documentation]     Gets status of EHR with given ehr_id.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_id` e.g.
    ...                             - `create new EHR`
    ...                             - `generate random ehr_id`

    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/ehr_status
                        ...         headers={"Content-Type": "application/json"}
                        # ...         headers={"If-Match": null}
                        Set Test Variable    ${response}    ${resp}

                        Output Debug Info To Console

                        Integer    response status    404
                        # String    response body error    EHR with this ID not found


set ehr_status of EHR
    [Documentation]     Sets status of EHR with given `ehr_id`.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_status` as JSON
    ...                             object e.g. `extract ehr_status from response (JSON)`

    # NOTE: alternatively u can save json to file and then pass this file to RESTinstance
    # ${ehrstatus}=       Load JSON From File   ehr_status.json
                        # Log To Console    ${ehr_status}
                        # Log To Console    ${ehr_status}[0]

    &{resp}=            REST.PUT    ${baseurl}/ehr/${ehr_id}/ehr_status    ${ehr_status}
                        ...         headers={"Content-Type": "application/json"}
                        ...         headers={"Prefer": "return=representation"}
                        ...         headers={"If-Match": "${ehrstatus_uid}"}

                                    # TODO: spec says "If-Match: {preceding_version_uid}"
                                    #       but we don't have this !!!
                                    # So what should be used as {preceding_version_uid} ???

                        Set Test Variable    ${response}    ${resp}

                        Output Debug Info To Console
                        Integer    response status    200


update ehr_status of fake EHR (w/o body)

    &{resp}=            REST.PUT    ${baseurl}/ehr/${ehr_id}/ehr_status
                    ...         headers={"Content-Type": "application/json"}
                    ...         headers={"Prefer": "return=representation"}
                    ...         headers={"If-Match": "${ehr_id}"}               # TODO: spec --> "If-Match: {preceding_version_uid}"
                    Set Test Variable    ${response}    ${resp}                 #       update If-Match value asap

                    Output Debug Info To Console
                    Integer    response status    404
                    String    response body error    EHR with this ID not found


update ehr_status of fake EHR (with body)

                        generate fake ehr_status

    &{resp}=            REST.PUT    ${baseurl}/ehr/${ehr_id}/ehr_status    ${ehr_status}
                        ...         headers={"Content-Type": "application/json"}
                        ...         headers={"Prefer": "return=representation"}
                        ...         headers={"If-Match": "${ehr_id}"}           # NOTE: spec --> "If-Match: {preceding_version_uid}"
                        Set Test Variable    ${response}    ${resp}             #       update If-Match value asap

                        Output Debug Info To Console
                        Integer    response status    404
                        # String    response body error    EHR with this ID not found


extract ehr_id from response (JSON)
    [Documentation]     Extracts ehr_id from response of preceding request.
    ...                 DEPENDENCY: `create new EHR`

    ${ehr_id}=          String       response body ehr_id value

                        Log To Console    \n\tDEBUG OUTPUT - EHR_ID: \n\t${ehr_id}[0]

                        Set Suite Variable    ${ehr_id}     ${ehr_id}[0]
                        # Set Test Variable    ${ehr_id}     ${response.body.ehr_id.value}    # same as above


extract system_id from response (JSON)
    [Documentation]     Extracts `system_id` from response of preceding request.
    ...                 DEPENDENCY: `create new EHR`

    ${system_id}=       String       response body system_id value

                        Log To Console    \n\tDEBUG OUTPUT - SYSTEM_ID: \n\t${system_id}[0]

                        Set Suite Variable    ${system_id}   ${system_id}[0]


extract subject_id from response (JSON)
    [Documentation]     Extracts subject_id from response of preceding request.

    Pass Execution    TEMP SOLUTION    broken_test    not-ready

    #TODO: @WLAD check Github Issue #272
    #      refactor this KW or it's usage in all test suites!

    #  ${subjectid}=      String      response body ehr_status subject external_ref id value
    #                     Log To Console    \n\tDEBUG OUTPUT - EHR_STATUS SUBJECT_ID: \n\t${subjectid}[0]
    #                     Set Suite Variable    ${subject_id}    ${subjectid}[0]


extract ehr_status from response (JSON)
    [Documentation]     Extracts ehr_status-object from response of preceding request.
    ...                 DEPENDENCY: `create new EHR`

    ${ehr_status}=      Object       response body ehr_status

                        Log To Console    \n\tDEBUG OUTPUT - EHR_STATUS:
                        Output       response body ehr_status

                        Set Suite Variable    ${ehr_status}     ${ehr_status}[0]


extract ehrstatus_uid (JSON)
    [Documentation]     Extracts uuid of ehr_status from response of preceding request.
    ...                 DEPENDENCY: `create new EHR`

    ${ehrstatus_uid}=   String       response body ehr_status uid value

                        Log To Console    \n\tDEBUG OUTPUT - EHR_STATUS UUID: \n\t${ehrstatus_uid}[0]
                        Set Suite Variable    ${ehrstatus_uid}   ${ehrstatus_uid}[0]


extract ehr_id from response (XML)
    [Documentation]     Extracts `ehr_id` from response of preceding request with content-type=xml
    ...                 DEPENDENCY: `create new EHR`

    ${xml}=             Parse Xml    ${response.body}
    ${ehr_id}=          Get Element Text    ${xml}    xpath=ehr_id/value
                        Set Test Variable   ${ehr_id}       ${ehr_id}


extract ehrstatus_uid (XML)
    [Documentation]     Extracts uuid of ehr_status from response of preceding request with content-type=xml
    ...                 DEPENDENCY: `create new EHR`

    ${xml}=             Parse Xml    ${response.body}
    ${ehrstatus_uid}=   Get Element Text    ${xml}    xpath=ehr_status/uid/value
                        Set Test Variable   ${ehrstatus_uid}    ${ehrstatus_uid}

extract system_id from response (XML)
    [Documentation]     Extracts `system_id` from response of preceding request with content-type=xml
    ...                 DEPENDENCY: `create new EHR`

    ${xml}=             Parse Xml    ${response.body}
    ${system_id}=       Get Element Text    ${xml}    xpath=system_id/value
                        Set Test Variable   ${system_id}    ${system_id}


create fake EHR
    generate random ehr_id
    generate random subject_id


generate random ehr_id
    [Documentation]     Generates a random UUIDv4 spec conform `ehr_id`
    ...                 and exposes it as Test Variable

    ${ehr_id}=          Evaluate    str(uuid.uuid4())    uuid
                        Set Test Variable    ${ehr_id}    ${ehr_id}


generate random subject_id
    [Documentation]     Generates a random UUIDv4 spec conform `subject_id`
    ...                 and exposes it as Test Variable

    ${subjectid}=       Evaluate    str(uuid.uuid4())    uuid
                        Set Test Variable    ${subject_id}    ${subjectid}


generate fake ehr_status
    [Documentation]     Loads a default ehr_status JSON object from fixtures folder
    ...                 and exposes it as Test Variable.

    ${json_ehr_status}=    Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec.json

                        Set Test Variable    ${ehr_status}    ${json_ehr_status}

                        Output    ${ehr_status}


set is_queryable / is_modifiable
    [Arguments]         ${is_modifiable}=${TRUE}    ${is_queryable}=${TRUE}
    [Documentation]     Sets boolean values of is_queryable / is_modifiable.
    ...                 Both default to ${TRUE},
    ...                 Valid Values: ${TRUE}, ${FALSE},
    ...                 DEPENDENCY: keywords that expose a `${ehr_status}` variable
    ...                 e.g. `generate fake ehr_status`

                        modify ehr_status is_modifiable to    ${is_modifiable}
                        modify ehr_status is_queryable to    ${is_queryable}


modify ehr_status is_queryable to
    [Arguments]         ${value}
    [Documentation]     Modifies `is_queryable` property of ehr_status JSON object
    ...                 and exposes  `ehr_status` Test Variable.
    ...                 DEPENDENCY: keywords that expose and `ehr_status` variable
    ...                 Valid values: `${FALSE}`, `${TRUE}`

    ${value}=           Set Variable If    $value=="true" or $value=="false"
                        ...    ${{bool(distutils.util.strtobool($value))}}
                        # comment: else leave it as is
                        ...    ${value}
    ${value}=           Set Variable If    $value=='"true"' or $value=='"false"'
                        ...    ${{$value.strip('"')}}
                        # comment: else
                        ...    ${value}
    ${value}=           Set Variable If    $value=="0" or $value=="1"
                        ...    ${{int($value)}}
                        # comment: else
                        ...    ${value}
    ${value}=           Set Variable If    $value=="null"
                        ...    ${{None}}
                        # comment: else
                        ...    ${value}
    ${value}=           Set Variable If    $value=='"null"'
                        ...    ${{$value.strip('"')}}
                        # comment: else
                        ...    ${value}
    ${ehr_status}=      Update Value To Json  ${ehr_status}  $..is_queryable  ${value}
                        # NOTE: alternatively u can save output to file
                        # Output   ${ehr_status}[0]             # ehr_status.json
                        Set Test Variable    ${ehr_status}    ${ehr_status}


modify ehr_status is_modifiable to
    [Arguments]         ${value}
    [Documentation]     Modifies `is_queryable` property of ehr_status JSON object
    ...                 and exposes  `ehr_status` Test Variable.
    ...                 DEPENDENCY: `get ehr_status from response`
    ...                 Valid values: `${FALSE}`, `${TRUE}`

    ${value}=           Set Variable If    $value=="true" or $value=="false"
                        ...    ${{bool(distutils.util.strtobool($value))}}
                        # comment: else
                        ...    ${value}
    ${value}=           Set Variable If    $value=='"true"' or $value=='"false"'
                        ...    ${{$value.strip('"')}}
                        # comment: else
                        ...    ${value}
    ${value}=           Set Variable If    $value=="0" or $value=="1"
                        ...    ${{int($value)}}
                        # comment: else
                        ...    ${value}
    ${value}=           Set Variable If    $value=="null"
                        ...    ${{None}}
                        # comment: else
                        ...    ${value}
    ${value}=           Set Variable If    $value=='"null"'
                        ...    ${{$value.strip('"')}}
                        # comment: else
                        ...    ${value}
    ${ehr_status}=      Update Value To Json  ${ehr_status}  $..is_modifiable  ${value}
                        # Output   ${ehr_status}[0]             # ehr_status.json
                        Set Test Variable    ${ehr_status}    ${ehr_status}


set ehr_status of EHR (from fixture)
    [Arguments]         ${fixture}
    [Documentation]     Sets status of EHR with given `ehr_id`.
    ...                 DEPENDENCY: `generate random ehr_id`

                        Set Headers    {"Prefer": "return=representation"}
                        Set Headers    {"Content-Type": "application/json"}
                        Set Headers    {"If-Match": "abc1234-none-exis-ting-fa8308e1242f::1"}

    &{resp}=            REST.PUT    ${baseurl}/ehr/${ehr_id}/ehr_status
    ...                             ${FIXTURES}/ehr/${fixture}
                        Set Test Variable    ${response}    ${resp}

                        # Output Debug Info To Console


Output Debug Info To Console
    [Documentation]     Prints all details of a request to console in JSON style.
    ...                 - request headers
    ...                 - request body
    ...                 - response headers
    ...                 - response body
    Output







# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]

# start request session
#     [Arguments]         ${content_type}
#     [Documentation]     Prepares settings for RESTInstace HTTP request.
#     Run Keyword If      "${content_type}"=="XML"   set content-type to XML
#     Run Keyword If      "${content_type}"=="JSON"  set content-type to JSON
#     Set Headers          ${authorization}
#     Set Headers          ${headers}

# set content-type to XML
#     [Documentation]     Set headers accept and content-type to XML.
#     ...                 DEPENDENCY: `start request session`
#     &{headers}=         Create Dictionary    Content-Type=application/xml
#                         ...                  Accept=application/xml
#                         ...                  Prefer=return=representation
#                         Set Test Variable    ${headers}    ${headers}

# set content-type to JSON
#     [Documentation]     Set headers accept and content-type to JSON.
#     ...                 DEPENDENCY: `start request session`
#     &{headers}=         Create Dictionary    Content-Type=application/json
#                         ...                  Accept=application/json
#                         ...                  Prefer=return=representation
#                         Set Test Variable    ${headers}    ${headers}
#                         # NOTE: EHRSCAPE fails to create EHR with `POST /ehr`
#                         #       when Content-Type=application/json is set
#                         #       But this is default header of RESTInstance lib!
#                         #       Do this "Content-Type=      " to unset it!      # BE AWARE!!


# create ehr without query params
#     [Arguments]  ${body}=None
#     &{resp}=    REST.POST    /ehr  body=${body}
#     Integer    response status    200  201  202  208  400
#     Set Test Variable    ${response}    ${resp}
#     [Teardown]  KE @Dev subjectId and subjectNamespace must be optional - tag(s): not-ready

# create ehr with query params
#     [Arguments]  ${queryparams}  ${body}=None
#     &{resp}=    REST.POST    /ehr?${queryparams}  body=${body}
#     Integer    response status    200  201  202  208  400
#     Set Test Variable    ${response}    ${resp}
#     [Teardown]  KE @Dev Not expected behavior - tag(s): not-ready

# extract ehrId
#
#     Log  DEPRECATION WARNING - @WLAD replace/remove this keyword!
#     ...  level=WARN
#
#     Set Test Variable    ${ehr_id}    ${response.body.ehrId}

# generate fake ehr_status with queryable = false
#
#     ${json_ehr_status}=     Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec.json
#     ${json_ehr_status}=     Update Value To Json  ${json_ehr_status}   $..is_queryable   false
#
#                             Set Test Variable    ${ehr_status}    ${json_ehr_status}
#
#                             Output    ${ehr_status}

# generate fake ehr_status with modifiable = false
#
#     ${json_ehr_status}=     Load JSON From File   ${FIXTURES}/ehr/ehr_status_1_api_spec.json
#     ${json_ehr_status}=     Update Value To Json  ${json_ehr_status}   $..is_modifiable   false
#
#                             Set Test Variable    ${ehr_status}    ${json_ehr_status}
#
#                             Output    ${ehr_status}

# extract ehrId XML
#     Set Test Variable    ${ehr_id}    ${response.body.ehr_id}

# verify ehrStatus queryable
#     [Arguments]   ${is_queryable}
#     ${QUERYALBE}=  Run Keyword If  "${is_queryable}"==""  Set Test Variable    ${is_queryable}    ${TRUE}
#     Boolean  $.ehrStatus.queryable  ${is_queryable}
#     #[Teardown]   Run keyword if  "${KEYWORD STATUS}"=="FAIL"  log a WARNING and set tag not-ready

# verify ehrStatus modifiable
#     [Arguments]   ${is_modifiable}
#     ${MODIFIABLE}=  Run Keyword If  "${is_modifiable}"==""  Set Test Variable    ${is_modifiable}    ${TRUE}
#     Boolean  $.ehrStatus.modifiable  ${is_modifiable}
#     #[Teardown]   Run keyword if  "${KEYWORD STATUS}"=="FAIL"  log a WARNING and set tag not-ready

# update ehr
#     [Arguments]    ${ehr_id}
#     &{resp}=    REST.PUT    /ehr/${ehr_id}/status    ${CURDIR}${/}../fixtures/ehr/update_body.json
#     Set Test Variable    ${response}    ${resp}
#     Integer    response status    200  401  403  404
#     # Output    response body

# get ehr by subject-id and namespace
#     [Arguments]    ${subject_id}    ${namespace}
#     &{resp}=    REST.GET    /ehr?subjectId=${subject_id}&subjectNamespace=${namespace}
#     Set Test Variable    ${response}    ${resp}

# get ehr by id
#     [Arguments]    ${ehr_id}
#     &{resp}=    REST.GET    /ehr/${ehr_id}
#     Set Test Variable    ${response}    ${resp}

# verify subject_id
#     [Arguments]    ${subject_id}
#     Should be equal as strings    ${subject_id}    ${response.body.ehrStatus['subjectId']}

# verify subject_namespace
#     [Arguments]    ${subject_namespace}
#     Should be equal as strings    ${subject_namespace}    ${response.body.ehrStatus['subjectNamespace']}

# verify response action
#     [Arguments]    ${action}
#     Should Be Equal As Strings    ${action}    ${response.body['action']}

# there is no EHR record
#     Log    Believe me - there is no record yet cause we started DB in a brand new docker container!


# #   ███████╗██╗  ██╗██████╗ ███████╗ ██████╗ █████╗ ██████╗ ███████╗
# #   ██╔════╝██║  ██║██╔══██╗██╔════╝██╔════╝██╔══██╗██╔══██╗██╔════╝
# #   █████╗  ███████║██████╔╝███████╗██║     ███████║██████╔╝█████╗
# #   ██╔══╝  ██╔══██║██╔══██╗╚════██║██║     ██╔══██║██╔═══╝ ██╔══╝
# #   ███████╗██║  ██║██║  ██║███████║╚██████╗██║  ██║██║     ███████╗
# #   ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝ ╚═════╝╚═╝  ╚═╝╚═╝     ╚══════╝
# #
# #   [ EHRSCAPE API SPECIFIC KEYWORDS ]
#
#
# extract ehr_id from response (JSON EHRSCAPE)
#     [Documentation]     Extracts ehr_id from response of executed request.
#     ...                 DEPENDENCY: `create new EHR`
#
#     ${ehr_id}=          String       response body ehr_id value
#                         # NOTE: RESTinstance returns ehr_id as a string IN A LIST
#                         #       Use index [0] to get string only
#                         Set Test Variable    ${ehr_id}     ${ehr_id}[0]
#                         Output   ${ehr_id}
#
#
# extract ehr_status from response (JSON EHRSCAPE)
#     [Documentation]     Extracts ehr_status from response of last request.
#     ...                 DEPENDENCY: `create new EHR`
#
#     ${ehr_status}=      Object       response body ehr_status
#                         # Output       response body ehr_status
#                         Set Test Variable    ${ehr_status}     ${ehr_status}[0]
#
#
# extract ehrstatus_uid (JSON EHRSCAPE)
#     [Documentation]     Extracts uid of ehr_status from response of last request.
#     ...                 DEPENDENCY: `create new EHR`
#
#     ${ehrstatus_uid}=   String       response body ehr_status uid value
#                         Output       response body ehr_status uid value
#                         # NOTE: RESTinstance returns uid as a string IN A LIST
#                         #       e.g. ['uid-123-xxx']
#                         #       Thus to get string only get item a index [0]
#                         Set Test Variable    ${ehrstatus_uid}   ${ehrstatus_uid}[0]
#
#
# extract ehr_id from response (XML EHRSCAPE)
#     [Documentation]     Extracts `ehr_id` from response with content-type=xml
#     ...                 DEPENDENCY: `create new EHR`
#
#             Log         DEPRECATION WARNING: @WLAD remove dat sh** when API-mess ends.
#             ...         level=WARN
#
#     ${xml}=             Parse Xml    ${response.body}
#     ${ehr_id}=          Get Element Text    ${xml}    xpath=ehr_id/value
#                         Set Test Variable   ${ehr_id}       ${ehr_id}
#
#
# extract system_id from response (JSON EHRSCAPE)
#     [Documentation]     Extracts `system_id` from response of executed request.
#     ...                 DEPENDENCY: `create new EHR`
#
#             Log         DEPRECATION WARNING: @WLAD remove dat sh** when API-mess ends.
#             ...         level=WARN
#
#     ${system_id}        String       response body systemId
#                         Set Test Variable    ${system_id}   ${system_id}[0]
#
#
# extract system_id from response (XML EHRSCAPE)
#     [Documentation]     Extracts `system_id` from response with content-type=xml
#     ...                 DEPENDENCY: `create new EHR`
#
#     ${xml}=             Parse Xml    ${response.body}
#     ${system_id}=       Get Element Text    ${xml}    xpath=systemId
#                         Set Test Variable   ${system_id}    ${system_id}
