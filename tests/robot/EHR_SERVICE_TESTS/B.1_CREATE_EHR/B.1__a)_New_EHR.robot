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
Metadata    Author    *Wladislaw Wagner*
Metadata    Created    2019.03.10

Documentation   B.1.a) Main flow: Create new EHR
...             https://docs.google.com/document/d/1r_z_E8MhlNdeVZS4xecl-8KbG0JPqCzKtKMfhuL81jY/edit#heading=h.yctjt73zw72p

Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags      refactor



*** Test Cases ***
000 - Default RESTInstance headers
    [Documentation]     Demonstrates default headers set by RESTInstance library
    ...                 which we use for many of our tests.
    ...                 Sets {"Content-Type": "applicable/json"} and 
    ...                      {"Accept": "application/json, */*"} by default!
    ...                 NOTE: this test is not executed on CI!
    [Tags]              libtest
    &{resp}=            REST.POST    ${baseurl}/ehr
                        Set Suite Variable    ${response}    ${resp}
                        Output Debug Info To Console
                        #Integer      response status    400


001 - Default RequestLibrary Headers
    [Documentation]     Demonstrates default headers set by RequestLibrary
    ...                 which we use in cases where RESTInstance can't be used because
    ...                 of missing XML support.
    ...                 Sets {"Accept": "*/*"} by default!
    ...                 NOTE: this test is not executed on CI!
    [Tags]              libtest
    ${resp}=            Post Request        ${SUT}   /ehr
                        #...                 headers=${headers}
                        Set Test Variable   ${response}    ${resp}
                        Log To Console   \nREQUEST HEADERS: \n${response.request.headers}
                        Log To Console   \nRESPONSE HEADERS: \n${response.headers}
                        Log To Console    \nRESPONSE BODY: ${response.content}


002 - POST /ehr (no accept header, content-type=json)
    [Tags]              libtest
    prepare new request session    no accept header
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


003 - POST /ehr (no accept header, content-type=xml)
    prepare new request session    no accept header xml
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


004 - POST /ehr (no content header)
    prepare new request session    no content header
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


005 - POST /ehr (no accept/content-type headers)
    prepare new request session    no accept/content headers
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


006 - POST /ehr (no headers)
    prepare new request session    no headers
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


MF-001 - Create new EHR (w/o Prefer header)
    [Tags]
    prepare new request session    JSON
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


MF-001a - Create new EHR (w/o Prefer and Accept header)
    [Tags]
    prepare new request session    no accept header
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


MF-002 - Create new EHR (Prefer header: minimal)
    [Tags]
    [Documentation]     This test should behave equqly to MF-001
    prepare new request session    JSON    Prefer=return=minimal
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


MF-003 - Create new EHR (XML, Prefer header: minimal)
    [Tags]
    prepare new request session    XML    Prefer=return=minimal
    # create new EHR (XML)
    create supernew ehr
    ehr_keywords.validate POST response - 204 no content


MF-004 - Create new EHR (Prefer header: representation)
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    create new EHR
    # comment: check step
    Object    response body


MF-005 - Create new EHR (XML, Prefer header: representation)
    [tags]    XML
    prepare new request session    XML    Prefer=return=representation
    create new EHR (XML)
    # comment: check steps
    String    response body    pattern=<?xml version
    String    response body    pattern=<ehr_id><value>
    String    response body    pattern=<ehr_status><name><value>EHR Status</value></name><uid


MF-006 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where provided ehr_staus is just empty json
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    POST /ehr    {}

    # comment: check step
    Integer    response status    400


MF-007 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where _type is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/000_ehr_status_type_missing.json
    POST /ehr    ${body}

    # comment: check step
    Integer    response status    400


MF-008 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mandatory subject is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    POST /ehr    ${EXECDIR}/robot/_resources/test_data_sets/ehr/invalid/001_ehr_status_subject_missing.json

    # comment: check step
    Integer    response status    400


MF-009 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where the following mandatory elements are missing:
    ...                 subject, archetype_node_id and name
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set
    ...          invalid/002_ehr_status_subject_and_archetype_and_name_missing.json
    POST /ehr    ${body}

    # comment: check step
    Integer    response status    400


MF-010 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mandatory subject..id is empty
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    POST /ehr    ${EXECDIR}/robot/_resources/test_data_sets/ehr/invalid/003_ehr_status_subject_id_empty.json

    # comment: check step
    Integer    response status    400


MF-011 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. subject..id is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    POST /ehr    ${EXECDIR}/robot/_resources/test_data_sets/ehr/invalid/004_ehr_status_subject_id_missing.json

    # comment: check step
    Integer    response status    400


MF-012 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. subject..namespace is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/005_ehr_status_subject_namespace_missing.json
    POST /ehr    ${body}

    # comment: check step
    Integer    response status    400


MF-013 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. subject..namespace is empty
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/006_ehr_status_subject_namespace_empty.json
    POST /ehr    ${body}

    # comment: check step
    Integer    response status    400


MF-014 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. is_modifiable is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/007_ehr_status_is_modifiable_missing.json
    POST /ehr    ${body}

        TRACE GITHUB ISSUE  154  not-ready

    # comment: check step
    Integer    response status    400


MF-015 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. is_queryable is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/008_ehr_status_is_queryable_missing.json
    POST /ehr    ${body}

        TRACE GITHUB ISSUE  154  not-ready

    # comment: check step
    Integer    response status    400


MF-016 - Create new EHR w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. is_modifiable and is_queryableis are missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/009_ehr_status_is_mod_and_is_quer_missing.json
    POST /ehr    ${body}

        TRACE GITHUB ISSUE  154  not-ready

    # comment: check step
    Integer    response status    400


MF-017 - Create new EHR w/ body: valid ehr_status
    [Documentation]     Covers happy path: valid ehr_status as body payload
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/000_ehr_status.json
    POST /ehr    ${body}
    Integer    response status    201


MF-018 - Create new EHR w/ body: valid ehr_status
    [Documentation]     Covers valid case where subject is empty JSON
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/001_ehr_status_subject_empty.json
    POST /ehr    ${body}

    Integer    response status    201


MF-019 - Create new EHR w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_TREE
    [Tags]              161
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/002_ehr_status_with_other_details_item_tree.json
    POST /ehr    ${body}
    Integer    response status    201

    ${actual_ehr_status}=    Object    response body ehr_status
    Set Test Variable    ${expected_ehr_status}    ${body}
    &{diff}=            compare json-strings    ${actual_ehr_status}[0]  ${expected_ehr_status}
                        ...    exclude_paths=root['uid']

        TRACE GITHUB ISSUE  161  not-ready

                        Log To Console    \n\n&{diff}
                        Should Be Empty    ${diff}    msg=DIFF DETECTED!


MF-020 - Create new EHR w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_LIST
    [Tags]              161
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/003_ehr_status_with_other_details_item_list.json
    POST /ehr    ${body}
    Integer    response status    201

    ${actual_ehr_status}=    Object    response body ehr_status
    Set Test Variable    ${expected_ehr_status}    ${body}

    &{diff}=            compare json-strings    ${actual_ehr_status}[0]  ${expected_ehr_status}
                        ...    exclude_paths=root['uid']
    
        TRACE GITHUB ISSUE  161  not-ready

                        Should Be Empty    ${diff}    msg=DIFF DETECTED!


MF-021 - Create new EHR w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_SINGLE
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/004_ehr_status_with_other_details_item_single.json
    POST /ehr    ${body}
    Integer    response status    201


MF-022 - Create new EHR w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_TABLE
    [Tags]              162
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/005_ehr_status_with_other_details_item_table.json
    POST /ehr    ${body}

        TRACE GITHUB ISSUE  162  not-ready

    Integer    response status    201


MF-023 - Create new EHR (POST /ehr variants)
    [Tags]
    [Template]          create ehr from data table

  # SUBJECT    IS_MODIFIABLE   IS_QUERYABLE   R.CODE
    given      ${EMPTY}        true           201
    given      ${EMPTY}        false          201
    given      false           ${EMPTY}       201
    given      true            ${EMPTY}       201
    given      true            true           201
    given      true            false          201
    given      false           false          201
    given      false           true           201

    given      null            null           201
    given      "null"          "null"         201
    given      "true"          "true"         201
    given      "false"         "false"        201
    given      1               1              201
    given      0               0              201


MF-024 - Create new EHR w/ empty subject (POST /ehr variants)
    [Documentation]     Covers edge case where subject is provided but is just empty JSON.
    [Tags]
    [Template]          create ehr from data table


  # SUBJECT    IS_MODIFIABLE   IS_QUERYABLE   R.CODE
    ${EMPTY}   ${EMPTY}        true           201
    ${EMPTY}   true            ${EMPTY}       201
    ${EMPTY}   true            true           201
    ${EMPTY}   ${EMPTY}        false          201
    ${EMPTY}   true            false          201
    ${EMPTY}   false           ${EMPTY}       201
    ${EMPTY}   false           true           201
    ${EMPTY}   false           false          201


MF-025 - Create new EHR w/ invalid subject (POST /ehr variants)
    [Documentation]     Covers edge case where subject is provided but is invalid
    ...                 because of some mandatory elements missing.
    [Tags]
    [Template]          create ehr from data table (invalid)

  # SUBJECT    IS_MODIFIABLE   IS_QUERYABLE   R.CODE
    # TODO: remove when fixed. Issue 158
    invalid    true            true           400
    invalid    true            false          400
    invalid    false           true           400
    invalid    false           false          400
    invalid    0               1              400

    # TODO: remove when fixed. Issue 157
    missing    true            true           400
    missing    true            false          400
    missing    false           true           400
    missing    false           false          400


MF-030 - Create new EHR w/ given ehr_id (PUT /ehr/ehr_id variants)
    [Tags]
    [Template]          create ehr w/ given ehr_id from data table

  # EHR_ID  SUBJECT    IS_MODIFIABLE   IS_QUERYABLE   R.CODE
    given   given      ${EMPTY}        true           201
    given   given      ${EMPTY}        false          201
    given   given      false           ${EMPTY}       201
    given   given      true            ${EMPTY}       201
    given   given      true            true           201
    given   given      true            false          201
    given   given      false           false          201
    given   given      false           true           201

    given   given      null            null           201
    given   given      "null"          "null"         201
    given   given      "true"          "true"         201
    given   given      "false"         "false"        201
    given   given      1               1              201
    given   given      0               0              201


MF-031 - Create new EHR w/ given ehr_id (PUT /ehr/ehr_id variants)
    [Tags]
    [Template]          create ehr w/ given ehr_id from data table

  # EHR_ID    SUBJECT    IS_MODIFIABLE   IS_QUERYABLE   R.CODE
    given     ${EMPTY}   true            true           201
    given     ${EMPTY}   true            false          201
    given     ${EMPTY}   false           false          201
    given     ${EMPTY}   false           true           201


MF-032 - Create new EHR w/ invalid ehr_id (PUT /ehr/ehr_id variants)
    [Tags]
    [Template]          create ehr w/ invalid ehr_id from data table

  # EHR_ID    SUBJECT    IS_MODIFIABLE   IS_QUERYABLE   R.CODE
    invalid   ${EMPTY}   true            true           400
    1234567   ${EMPTY}   ${EMPTY}        true           400
    .......   ${EMPTY}   ${EMPTY}        false          400
    0000000   ${EMPTY}   false           ${EMPTY}       400


MF-033 - Create new EHR w/ given ehr_id (w/o Prefer header)
    [Tags]
    # TODO: @WLAD update as soon as RESTInstance allows unsetting/clearing headers
    #       remove "Prefer=${None}"
    prepare new request session    JSON    Prefer=${None}
    PUT /ehr/$ehr_id
    ehr_keywords.validate PUT response - 204 no content


MF-034 - Create new EHR w/ given ehr_id (Prefer header: minimal)
    [Tags]
    [Documentation]     This test should behave equqly to MF-033
    prepare new request session    JSON    Prefer=return=minimal
    PUT /ehr/$ehr_id
    ehr_keywords.validate PUT response - 204 no content


MF-035 - Create new EHR w/ given ehr_id (XML, Prefer header: minimal)
    [Tags]
    prepare new request session    XML    Prefer=return=minimal
    PUT /ehr/$ehr_id
    ehr_keywords.validate PUT response - 204 no content


MF-036 - Create new EHR w/ given ehr_id (Prefer header: representation)
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    PUT /ehr/$ehr_id
    # comment: check step
    Object    response body


MF-037 - Create new EHR w/ given ehr_id (XML, Prefer header: representation)
    [tags]    XML
    prepare new request session    XML    Prefer=return=representation
    PUT /ehr/$ehr_id
    # comment: check steps
    String    response body    pattern=<?xml version
    String    response body    pattern=<ehr_id><value>
    String    response body    pattern=<ehr_status><name><value>EHR Status</value></name><uid


MF-038 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where provided ehr_staus is just empty json
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    PUT /ehr/$ehr_id    body={}

    # comment: check step
    Integer    response status    400


MF-039 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where _type is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/000_ehr_status_type_missing.json
    PUT /ehr/$ehr_id    body=${body}

    # comment: check step
    Integer    response status    400


MF-040 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mandatory subject is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    PUT /ehr/$ehr_id    body=${EXECDIR}/robot/_resources/test_data_sets/ehr/invalid/001_ehr_status_subject_missing.json

    # comment: check step
    Integer    response status    400


MF-041 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where the following mandatory elements are missing:
    ...                 subject, archetype_node_id and name
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/002_ehr_status_subject_and_archetype_and_name_missing.json
    PUT /ehr/$ehr_id    body=${body}

    # comment: check step
    Integer    response status    400


MF-042 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mandatory subject..id is empty
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    PUT /ehr/$ehr_id    body=${EXECDIR}/robot/_resources/test_data_sets/ehr/invalid/003_ehr_status_subject_id_empty.json

    # comment: check step
    Integer    response status    400


MF-043 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. subject..id is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    PUT /ehr/$ehr_id    body=${EXECDIR}/robot/_resources/test_data_sets/ehr/invalid/004_ehr_status_subject_id_missing.json

    # comment: check step
    Integer    response status    400


MF-044 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. subject..namespace is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/005_ehr_status_subject_namespace_missing.json
    PUT /ehr/$ehr_id    body=${body}

    # comment: check step
    Integer    response status    400


MF-045 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. subject..namespace is empty
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/006_ehr_status_subject_namespace_empty.json
    PUT /ehr/$ehr_id    body=${body}

    # comment: check step
    Integer    response status    400


MF-046 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. is_modifiable is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/007_ehr_status_is_modifiable_missing.json
    PUT /ehr/$ehr_id    body=${body}

        TRACE GITHUB ISSUE  154  not-ready

    # comment: check step
    Integer    response status    400


MF-047 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. is_queryable is missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/008_ehr_status_is_queryable_missing.json
    PUT /ehr/$ehr_id    body=${body}

        TRACE GITHUB ISSUE  154  not-ready

    # comment: check step
    Integer    response status    400


MF-048 - Create new EHR w/ given ehr_id w/ body: invalid ehr_status
    [Documentation]     Covers case where mand. is_modifiable and is_queryableis are missing
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    invalid/009_ehr_status_is_mod_and_is_quer_missing.json
    PUT /ehr/$ehr_id    body=${body}

        TRACE GITHUB ISSUE  154  not-ready

    # comment: check step
    Integer    response status    400


MF-049 - Create new EHR w/ given ehr_id w/ body: valid ehr_status
    [Documentation]     Covers happy path: valid ehr_status as body payload
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/000_ehr_status.json
    PUT /ehr/$ehr_id    body=${body}
    Integer    response status    201


MF-050 - Create new EHR w/ given ehr_id w/ body: valid ehr_status
    [Documentation]     Covers valid case where subject is empty JSON
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/001_ehr_status_subject_empty.json
    PUT /ehr/$ehr_id    body=${body}

    Integer    response status    201


MF-051 - Create new EHR w/ given ehr_id w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_TREE
    [Tags]              161
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/002_ehr_status_with_other_details_item_tree.json
    PUT /ehr/$ehr_id    body=${body}
    Integer    response status    201

    ${actual_ehr_status}=    Object    response body ehr_status
    Set Test Variable    ${expected_ehr_status}    ${body}

    &{diff}=            compare json-strings    ${actual_ehr_status}[0]  ${expected_ehr_status}
                        ...    exclude_paths=root['uid']

        TRACE GITHUB ISSUE  161  not-ready

                        Should Be Empty    ${diff}    msg=DIFF DETECTED!


MF-052 - Create new EHR w/ given ehr_id w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_LIST
    [Tags]              161
    prepare new request session    JSON
    ${body}=     randomize subject_id in test-data-set    valid/003_ehr_status_with_other_details_item_list.json
    PUT /ehr/$ehr_id    body=${body}
    Integer    response status    201

    ${actual_ehr_status}=    Object    response body ehr_status
    Set Test Variable    ${expected_ehr_status}    ${body}

    &{diff}=            compare json-strings    ${actual_ehr_status}[0]  ${expected_ehr_status}
                        ...    exclude_paths=root['uid']

        TRACE GITHUB ISSUE  161  not-ready

                        Should Be Empty    ${diff}    msg=DIFF DETECTED!


MF-053 - Create new EHR w/ given ehr_id w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_SINGLE
    [Tags]
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/004_ehr_status_with_other_details_item_single.json
    PUT /ehr/$ehr_id    body=${body}
    Integer    response status    201


MF-054 - Create new EHR w/ given ehr_id w/ body: valid ehr_status w/ o.d.
    [Documentation]     Covers happy path w/ "other_details" _type ITEM_TABLE
    [Tags]              162
    prepare new request session    JSON    Prefer=return=representation
    ${body}=     randomize subject_id in test-data-set    valid/005_ehr_status_with_other_details_item_table.json
    PUT /ehr/$ehr_id    body=${body}

        TRACE GITHUB ISSUE  162  not-ready

    Integer    response status    201





*** Keywords ***
create ehr from data table
    [Arguments]         ${subject}  ${is_modifiable}  ${is_queryable}  ${status_code}

                        prepare new request session    Prefer=return=representation

    &{resp}=            Run Keywords
                        ...    compose ehr_status    ${subject}    ${is_modifiable}    ${is_queryable}  AND
                        ...    POST /ehr    ${ehr_status}  AND
                        ...    check response    ${status_code}    ${is_modifiable}    ${is_queryable}


create ehr from data table (invalid)
    [Arguments]         ${subject}  ${is_modifiable}  ${is_queryable}  ${status_code}

                        prepare new request session    Prefer=return=representation

    &{resp}=            Run Keywords
                        ...    compose ehr_status    ${subject}    ${is_modifiable}    ${is_queryable}  AND
                        ...    POST /ehr    ${ehr_status}  AND
                        ...    check response (invalid)    ${status_code}


create ehr w/ given ehr_id from data table
    [Arguments]         ${ehr_id}  ${subject}  ${is_modifiable}  ${is_queryable}  ${status_code}

                        prepare new request session    Prefer=return=representation

    ${ehr_id}=          Set Variable If    $ehr_id=="given"    ${{str(uuid.uuid4())}}    invalid_ehr_id
    &{resp}=            Run Keywords
                        ...    compose ehr_status    ${subject}    ${is_modifiable}    ${is_queryable}  AND
                        ...    PUT /ehr/$ehr_id    ${ehr_id}    ${ehr_status}  AND
                        ...    check response    ${status_code}    ${is_modifiable}    ${is_queryable}


create ehr w/ invalid ehr_id from data table
    [Arguments]         ${ehr_id}  ${subject}  ${is_modifiable}  ${is_queryable}  ${status_code}

                        prepare new request session    Prefer=return=representation

    ${ehr_id}=          Set Variable    ${ehr_id}
    &{resp}=            Run Keywords
                        ...    compose ehr_status    ${subject}    ${is_modifiable}    ${is_queryable}  AND
                        ...    PUT /ehr/$ehr_id    ${ehr_id}    ${ehr_status}  AND
                        ...    check response (invalid ehr_id)    ${status_code}


compose ehr_status
    [Arguments]         ${subject}    ${is_modifiable}    ${is_queryable}

                        set ehr_status subject    ${subject}
                        set is_queryable / is_modifiable    ${is_modifiable}    ${is_queryable}
                        Set Test Variable    ${ehr_status}    ${ehr_status}


set ehr_status subject
    [Arguments]         ${subject}

    ${ehr_status}=      Load JSON From File    ${EXECDIR}/robot/_resources/test_data_sets/ehr/valid/000_ehr_status.json
                        add random subject_id to ehr_status    ${ehr_status}

                        # comment: valid but empty subject
                        Run Keyword And Return If    $subject=="${EMPTY}"
                        ...    delete subject.external_ref from ehr_status    ${ehr_status}

                        # commment: missing subject (makes ehr_status invalid)
                        Run Keyword And Return If    $subject=="missing"
                        ...    delete subject from ehr_status    ${ehr_status}

                        # comment: invalid subject (makes ehr_status invalid)
                        Run Keyword And Return If    $subject=="invalid"
                        ...    create randomly invalid subject    ${ehr_status}

                        # comment: else - a valid ehr_status w/ random subject..id is exposed
                        Set Test Variable    ${ehr_status}    ${ehr_status}


create randomly invalid subject
    [Documentation]     Creates an invalid subject object by removing some mandatory
    ...                 elements from valid subject object randomly
    [Arguments]         ${ehr_status}

    ${subj_elements}=   Create List
                        ...   $..subject.external_ref.id.value
                        ...   $..subject.external_ref.id
                        ...   $..subject.external_ref.id._type
                        #...   $..subject.external_ref.id.schema    # not necessary for the most abstract OBJECT_ID type
                        ...   $..subject.external_ref.namespace

    ${elem_to_delete}=  Set Variable    ${{random.choice($subj_elements)}}

    Log To Console    ${elem_to_delete}

    ${ehr_status}=      Delete Object From Json    ${ehr_status}    ${elem_to_delete}
                        Set Test Variable    ${ehr_status}    ${ehr_status}


delete subject from ehr_status
    [Arguments]         ${ehr_status}
    ${ehr_status}=      Delete Object From Json  ${ehr_status}   $..subject
                        Set Test Variable    ${ehr_status}    ${ehr_status}


delete subject.external_ref from ehr_status
    [Documentation]     Creates and exposes an ehr_status w/ an empty but valid subject
    [Arguments]         ${ehr_status}

    ${ehr_status}=      Delete Object From Json    ${ehr_status}    $..subject.external_ref
                        Set Test Variable    ${ehr_status}    ${ehr_status}

        # # alternative implementation
        # ${empty_subj}=      Create Dictionary    &{EMPTY}
        # ${ehr_status}=      Update Value To Json     ${ehr_status}     $..subject    ${empty_subj}
        #                     Set Test Variable    ${ehr_status}    ${ehr_status}


add random subject_id to ehr_status
    [Documentation]     Updates $..subject.external_ref.id.value with random uuid
    [Arguments]         ${ehr_status}

    ${subject_id}=      generate random id
    ${ehr_status}=      Update Value To Json     ${ehr_status}    $..subject.external_ref.id.value  ${subject_id}
                        Set Test Variable    ${ehr_status}    ${ehr_status}


randomize subject_id in test-data-set
    [Arguments]         ${test_data_set}
    ${subject_id}=      generate random id
    ${body}=            Load JSON From File    ${EXECDIR}/robot/_resources/test_data_sets/ehr/${test_data_set}
    ${body}=            Update Value To Json    ${body}  $..subject.external_ref.id.value  ${subject_id}
    [RETURN]            ${body}


generate random id
    # ${uuid}=            Evaluate    str(uuid.uuid4())    uuid
    ${uuid}=            Set Variable    ${{str(uuid.uuid4())}}
    [RETURN]            ${uuid}


POST /ehr
    [Arguments]         ${body}=${None}
    &{response}=        REST.POST    /ehr    ${body}
                        Output Debug Info To Console


PUT /ehr/$ehr_id
    [Arguments]         ${ehr_id}=${{str(uuid.uuid4())}}    ${body}=${None}
    &{response}=        REST.PUT    /ehr/${ehr_id}    ${body}
                        Output Debug Info To Console
    # [RETURN]            ${response}


check response
    [Arguments]         ${status_code}    ${is_modifiable}    ${is_queryable}
                        Integer    response status    ${status_code}

    # comment: changes is_modif./is_quer. to default expected values - boolean true
    ${is_modifiable}=   Run Keyword If    $is_modifiable=="${EMPTY}"    Set Variable    ${TRUE}
    ${is_queryable}=    Run Keyword If  $is_queryable=="${EMPTY}"    Set Variable    ${TRUE}
                        Boolean    response body ehr_status is_modifiable    ${is_modifiable}
                        Boolean    response body ehr_status is_queryable    ${is_queryable}


check response (invalid)
    [Arguments]         ${status_code}
                        Integer    response status    ${status_code}


check response (invalid ehr_id)
    [Arguments]         ${status_code}
                        Integer    response status    ${status_code}
                        String    response body error    EHR ID format not a UUID
