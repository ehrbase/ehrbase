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
Metadata    Created    2019.02.26

Documentation   B.1.a) Main flow: Create new EHR
...             temp TEST SUITE according to JIRA TICKET EHR-3
...             should be removed as soon as POST support proper setting
...             of is_queryable and is_modifiable

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

Suite Setup    startup SUT
Suite Teardown    shutdown SUT
Test Template    client sends POST request to /ehr

Force Tags    create_ehr    obsolete


#                                                       | ------ EHR STATUS ----- |
*** Test Cases ***           SUBJECT_ID  SUBJECT_NMSP.  IS_QUERYABLE  IS_MODIFIABLE  R.CODE
Create EHR - Data Set #29    given125    given125       ${EMPTY}      false          201
Create EHR - Data Set #30    given126    given126       true          false          201
Create EHR - Data Set #33    given127    given127       false         ${EMPTY}       201
Create EHR - Data Set #34    given128    given128       false         true           201
Create EHR - Data Set #36    given129    given129       false         false          201



Retrieve EHR and Check Consistence with Test Data Set
    [Documentation]  This tests Double-check the results from the tests above
    [Template]  client sends GET request to /ehr{?subject_id,subject_namespace}
    [Tags]

  # SUBJECT_ID  SUBJECT_NMSP.  IS_QUERYABLE  IS_MODIFIABLE  R.CODE  # DATASET
    given125    given125       ${EMPTY}      false          200     # Data Set #29
    given126    given126       true          false          200     # Data Set #30
    given127    given127       false         ${EMPTY}       200     # Data Set #33
    given128    given128       false         true           200     # Data Set #34
    given129    given129       false         false          200     # Data Set #36



*** Keywords ***
client sends POST request to /ehr
    [Arguments]  ${subject_id}  ${subject_namespace}  ${is_queryable}  ${is_modifiable}  ${status_code}

    # STEP 1 - Invoke the create EHR service (for each item in the Data set)
    &{R}=    REST.POST  /ehr?subjectId=${subject_id}&subjectNamespace=${subject_namespace}  # body=${body}
    Set Test Variable    ${response}    ${R}
    Run Keyword And Continue On Failure  verify response action  CREATE

    # STEP 2 - The server should answer with a positive response associated to "EHR created"
    Integer   response status    ${status_code}
    extract ehrId

    # STEP EXTRA
    # NOTE: this is a workaround for POST not being able to set is_queryable & is_modifiable properly.
    #       we set the values by using the PUT method
    # prepare body
    build body    ${is_queryable}    ${is_modifiable}

    &{R}=   REST.PUT  /ehr/${ehr_id}/status  body=${body}
    Set Test Variable    ${response}    ${R}
    Integer   response status  200


    # STEP 3 - Postcondition: New EHR will be consistent with the data sets used
    Run Keyword And Continue On Failure  verify subject_id  ${subject_id}
    Run Keyword And Continue On Failure  verify subject_namespace  ${subject_namespace}
    Run Keyword And Continue On Failure  verify response action  UPDATE
    Run Keyword And Continue On Failure  verify ehrStatus queryable    ${is_queryable}
    Run Keyword And Continue On Failure  verify ehrStatus modifiable    ${is_modifiable}

    [Teardown]  KW Wrong Status Code! - tag(s): not-ready


client sends GET request to /ehr{?subject_id,subject_namespace}
    [Arguments]  ${subject_id}  ${subject_namespace}  ${is_queryable}  ${is_modifiable}  ${status_code}
    &{R}=    REST.GET    /ehr?subjectId=${subject_id}&subjectNamespace=${subject_namespace}
    Set Test Variable    ${response}    ${R}
    Integer   response status    ${status_code}

    # New EHR will be consistent with the data sets used
    Run Keyword And Continue On Failure  verify subject_id  ${subject_id}
    Run Keyword And Continue On Failure  verify subject_namespace  ${subject_namespace}
    Run Keyword And Continue On Failure  verify response action  RETRIEVE
    Run Keyword And Continue On Failure  verify ehrStatus queryable    ${is_queryable}
    Run Keyword And Continue On Failure  verify ehrStatus modifiable    ${is_modifiable}
    [Teardown]


build body
    [Arguments]   ${is_queryable}  ${is_modifiable}
    ${body}=  Run Keyword If  "${is_queryable}"=="" and "${is_modifiable}"==""  Set Variable  {}
    ...    ELSE IF  "${is_queryable}"==""  Set Variable  { "modifiable": ${is_modifiable} }
    ...    ELSE IF  "${is_modifiable}"==""  Set Variable  { "queryable": ${is_queryable} }
    ...    ELSE  Set Variable  { "queryable": ${is_queryable}, "modifiable": ${is_modifiable} }
    Set Test Variable  ${body}  ${body}
