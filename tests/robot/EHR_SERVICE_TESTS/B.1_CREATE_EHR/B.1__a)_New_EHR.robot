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
Documentation   B.1.a) Main flow: Create new EHR
...             https://docs.google.com/document/d/1r_z_E8MhlNdeVZS4xecl-8KbG0JPqCzKtKMfhuL81jY/edit#heading=h.yctjt73zw72p

Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags    refactor



*** Test Cases ***
MF-001 - Create new EHR (w/o Prefer header)
    [Tags]    not-ready   xxx
    prepare new request session    JSON
    create new EHR

        TRACE GITHUB ISSUE  143  not-ready

    # comment: check step
    Null   response body


MF-002 - Create new EHR (Prefer header: minimal)
    [Tags]    not-ready
    [Documentation]     This test should behave equqly to MF-001
    prepare new request session    JSON    Prefer=return=minimal
    create new EHR

        TRACE GITHUB ISSUE  143  not-ready

    # comment: check step
    Null   response body


MF-003 - Create new EHR (XML, Prefer header: minimal)
    prepare new request session    XML    Prefer=return=minimal
    create new EHR (XML)

        TRACE GITHUB ISSUE  143  not-ready

    # TODO: @WLAD create step to check body is null
    Null   response body    # not sure this works with XML response


MF-004 - Create new EHR (Prefer header: representation)
    prepare new request session    JSON    Prefer=return=representation
    create new EHR
    # comment: check step
    Object    response body


MF-005 - Create new EHR (XML, Prefer header: representation)
    prepare new request session    XML    Prefer=return=representation
    create new EHR (XML)


MF-006 - Create new EHR (w/ body: empty json)
    [Tags]    not-ready   xxx
    prepare new request session    JSON
    # create new EHR
    POST /ehr    {}    201


MF-007 - Create new EHR (POST /ehr variants)
    [Tags]              not-ready     xxx
    [Template]          create ehr

  # EHR_ID      SUBJECT_ID  SUBJECT_NMSP.  IS_QUERYABLE  IS_MODIFIABLE  R.CODE  BODY
    ${EMPTY}    ${EMPTY}    ${EMPTY}       ${EMPTY}      true           201     002_ehr_status_no_subject.json
    ${EMPTY}    ${EMPTY}    ${EMPTY}       true          ${EMPTY}       201     002_ehr_status_no_subject.json
    ${EMPTY}    ${EMPTY}    ${EMPTY}       true          true           201     002_ehr_status_no_subject.json
    ${EMPTY}    ${EMPTY}    ${EMPTY}       ${EMPTY}      false          201     002_ehr_status_no_subject.json
    ${EMPTY}    ${EMPTY}    ${EMPTY}       true          false          201     002_ehr_status_no_subject.json
    ${EMPTY}    ${EMPTY}    ${EMPTY}       false         ${EMPTY}       201     002_ehr_status_no_subject.json
    ${EMPTY}    ${EMPTY}    ${EMPTY}       false         true           201     002_ehr_status_no_subject.json
    ${EMPTY}    ${EMPTY}    ${EMPTY}       false         false          201     002_ehr_status_no_subject.json

    # ${EMPTY}    given       given          ${EMPTY}      true           201     YES     # post /ehr + body
    # ${EMPTY}    given       given          true          ${EMPTY}       201     YES     # post /ehr + body
    # ${EMPTY}    given       given          ${EMPTY}      ${EMPTY}       201     YES     # post /ehr + body
    # ${EMPTY}    given       given          true          true           201     YES     # post /ehr + body
    # ${EMPTY}    given       given          ${EMPTY}      false          201     YES     # post /ehr + body
    # ${EMPTY}    given       given          true          false          201     YES     # post /ehr + body
    # ${EMPTY}    given       given          false         ${EMPTY}       201     YES     # post /ehr + body
    # ${EMPTY}    given       given          false         true           201     YES     # post /ehr + body
    # ${EMPTY}    given       given          false         false          201     YES     # post /ehr + body

    # ${EMPTY}    ${EMPTY}    given          ${EMPTY}      ${EMPTY}       400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          ${EMPTY}      true           400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          true          ${EMPTY}       400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          true          true           400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          ${EMPTY}      false          400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          true          false          400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          false         ${EMPTY}       400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          false         true           400     YES     # post /ehr + body
    # ${EMPTY}    ${EMPTY}    given          false         false          400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       false         false          400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       ${EMPTY}      false          400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       true          false          400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       false         ${EMPTY}       400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       false         true           400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       ${EMPTY}      true           400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       true          ${EMPTY}       400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       ${EMPTY}      ${EMPTY}       400     YES     # post /ehr + body
    # ${EMPTY}    given       ${EMPTY}       true          true           400     YES     # post /ehr + body



MF-007 - Create new EHR w/ given ehr_id (PUT /ehr variants)
    [Tags]              not-ready         
    [Template]          create ehr w/ given ehr_id

  # EHR_ID      SUBJECT_ID  SUBJECT_NMSP.  IS_QUERYABLE  IS_MODIFIABLE  R.CODE  w/BODY  REQUEST
    given    ${EMPTY}    ${EMPTY}       ${EMPTY}      ${EMPTY}       201        NO      # put /ehr
    given    ${EMPTY}    ${EMPTY}       ${EMPTY}      true           201        YES     # put /ehr + body
    given    ${EMPTY}    ${EMPTY}       true          ${EMPTY}       201        YES     # put /ehr + body
    given    ${EMPTY}    ${EMPTY}       true          true           201        YES     # put /ehr + body
    given    given       ${EMPTY}       ${EMPTY}      true           400        YES     # put /ehr + body
    given    given       ${EMPTY}       true          ${EMPTY}       400        YES     # put /ehr + body
    given    given       ${EMPTY}       ${EMPTY}      ${EMPTY}       400        YES     # put /ehr + body
    given    given       ${EMPTY}       true          true           400        YES     # put /ehr + body
    given    ${EMPTY}    ${EMPTY}       ${EMPTY}      false          201        YES     # put /ehr + body
    given    ${EMPTY}    ${EMPTY}       true          false          201        YES     # put /ehr + body
    given    given       ${EMPTY}       ${EMPTY}      false          400        YES     # put /ehr + body
    given    given       ${EMPTY}       true          false          400        YES     # put /ehr + body
    given    ${EMPTY}    ${EMPTY}       false         ${EMPTY}       201        YES     # put /ehr + body
    given    ${EMPTY}    ${EMPTY}       false         true           201        YES     # put /ehr + body
    given    given001    ${EMPTY}       false         ${EMPTY}       400        YES     # put /ehr + body
    given    given001    ${EMPTY}       false         true           400        YES     # put /ehr + body
    given    ${EMPTY}    ${EMPTY}       false         false          201        YES     # put /ehr + body
    given    given       ${EMPTY}       false         false          400        YES     # put /ehr + body
    given    ${EMPTY}    given          ${EMPTY}      ${EMPTY}       400        YES     # put /ehr + body
    given    ${EMPTY}    given          ${EMPTY}      true           400        YES     # put /ehr + body
    given    ${EMPTY}    given          true          ${EMPTY}       400        YES     # put /ehr + body
    given    ${EMPTY}    given          true          true           400        YES     # put /ehr + body
    given    given       given          ${EMPTY}      true           201        YES     # put /ehr + body
    given    given       given          true          ${EMPTY}       201        YES     # put /ehr + body
    given    given       given          ${EMPTY}      ${EMPTY}       201        YES     # put /ehr + body
    given    given       given          true          true           201        YES     # put /ehr + body
    given    ${EMPTY}    given          ${EMPTY}      false          400        YES     # put /ehr + body
    given    ${EMPTY}    given          true          false          400        YES     # put /ehr + body
    given    given       given          ${EMPTY}      false          201        YES     # put /ehr + body
    given    given       given          true          false          201        YES     # put /ehr + body
    given    ${EMPTY}    given          false         ${EMPTY}       400        YES     # put /ehr + body
    given    ${EMPTY}    given          false         true           400        YES     # put /ehr + body
    given    given       given          false         ${EMPTY}       201        YES     # put /ehr + body
    given    given       given          false         true           201        YES     # put /ehr + body
    given    ${EMPTY}    given          false         false          400        YES     # put /ehr + body
    given    given       given          false         false          201        YES     # put /ehr + body



*** Keywords ***
create ehr
    [Arguments]  ${ehr_id}  ${subj_id}  ${subj_namespace}  ${is_queryable}  ${is_modifiable}  ${status_code}  ${body}

    prepare new request session
    compose valid body    ${is_queryable}    ${is_modifiable}

    &{response}=        Run Keyword If  "${body}"=="002_ehr_status_no_subject.json"  POST /ehr  $payload  status

    # &{resp}=    Run Keyword If  "${w/body}"=="NO"    POST /ehr    None    ${status_code}
    # ...         ELSE IF  "${w/body}"=="YES"    POST /ehr    ${body}    ${status_code}
    # ...         ELSE IF  "${w/body}"=="YES" and "${is_queryable}"=="true"    POST /ehr    ${body}    ${status_code}
    # ...         ELSE IF  "${w/body}"=="YES" and "${is_modifiable}"=="true" and "${is_queryable}"==true
    #             ...       POST /ehr    ${body}    ${status_code}

    # ...         ELSE IF  "${subject_namespace}"==""  create ehr with query params  subjectId\=${subject_id}  body=${body}
    # ...    ELSE IF  "${ehr_id}"!="" and "${subject_id}"=="" and "${subject_namespace}"==""  REST.PUT  /ehr/${ehr_id}  body=${body}
    # ...    ELSE IF  "${ehr_id}"!="" and "${subject_id}"==""  REST.PUT  /ehr/${ehr_id}?subjectNamespace=${subject_namespace}  body=${body}
    # ...    ELSE IF  "${ehr_id}"!="" and "${subject_namespace}"==""  REST.PUT  /ehr/${ehr_id}?subjectId=${subject_id}  body=${body}
    # ...    ELSE IF  "${ehr_id}"!="" and "${subject_id}"!="" and "${subject_namespace}"!=""  REST.PUT  /ehr/${ehr_id}?subjectId=${subject_id}&subjectNamespace=${subject_namespace}  body=${body}
    # ...    ELSE  REST.POST  /ehr?subjectId=${subject_id}&subjectNamespace=${subject_namespace}  body=${body}
    Set Test Variable    ${response}    ${resp}


create ehr w/ given ehr_id
    [Arguments]  ${ehr_id}  ${subj_id}  ${subj_namespace}  ${is_queryable}  ${is_modifiable}  ${status_code}  ${w/body}

    prepare new request session
    compose valid body    ${is_queryable}    ${is_modifiable}

    &{resp}=    Run Keyword If  "${w/body}"=="NO"    PUT /ehr    None    ${status_code}
    ...         ELSE IF  "${w/body}"=="YES" and "${is_modifiable}"=="true"    PUT /ehr    ${body}    ${status_code}
    ...         ELSE IF  "${w/body}"=="YES" and "${is_queryable}"=="true"    PUT /ehr    ${body}    ${status_code}
    ...         ELSE IF  "${w/body}"=="YES" and "${is_modifiable}"=="true" and "${is_queryable}"==true
                ...       PUT /ehr    ${body}    ${status_code}


POST /ehr
    [Arguments]         ${body}    ${status}
    &{response}=        REST.POST    /ehr    ${body}
                        Output Debug Info To Console

                        Integer    response status    ${status}


PUT /ehr
    [Arguments]         ${body}    ${status}
    &{response}=        REST.PUT    /ehr    ${body}
                        Output Debug Info To Console

                        Integer    response status    ${status}

    001_ehr_status.json


compose valid body
    [Arguments]         ${is_queryable}  ${is_modifiable}

    ${subject_id}=      generate random id

    ${body}=            Load JSON From File    ${EXECDIR}/robot/_resources/test_data_sets/ehr/valid/002_ehr_status_no_subject.json
    ${body}=            Update Value To Json  ${body}  $..is_modifiable  ${is_modifiable}
    ${body}=            Update Value To Json  ${body}  $..is_queryable  ${is_queryable}
    ${body}=            Update Value To Json  ${body}  $..subject.external_ref.id.value  ${subject_id}

    ${body}=  Run Keyword If  "${is_queryable}"=="" and "${is_modifiable}"==""  Set Variable    None
    # ...    ELSE IF  "${is_queryable}"==""  Set Variable  { "is_modifiable": ${is_modifiable} }
    ...    ELSE IF  "${is_queryable}"==""  Set Variable  ${body}
    ...    ELSE IF  "${is_modifiable}"==""  Set Variable  ${body}
    ...    ELSE  Set Variable  ${body}
    Set Test Variable  ${body}  ${body}


generate random id
    ${uuid}=            Evaluate    str(uuid.uuid4())    uuid
    [RETURN]            ${uuid}


create ehr without query params
    [Arguments]         ${body}=None
    &{resp}=            REST.POST    /ehr  body=${body}
                        Output Debug Info To Console
                        Integer    response status    201
                        Set Test Variable    ${response}    ${resp}


create ehr with query params
    [Arguments]         ${queryparams}  ${body}=None
    &{resp}=            REST.POST    /ehr?${queryparams}    body=${body}
                        Integer    response status    201
                        Set Test Variable    ${response}    ${resp}
