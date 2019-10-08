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

Documentation   A.1.a) Main flow: Create new EHR
...
...             https://vitasystemsgmbh.atlassian.net/wiki/spaces/ETHERCIS/pages/498532998/EHR+Test+Suite#EHRTestSuite-1.CreateEHR
...             Conformance Point: I_EHR_SERVICE / .create_ehr()
...             https://specifications.openehr.org/releases/CNF/latest/openehr_platform_conformance.html#_ehr_persistence_component
...

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

Suite Setup    startup SUT
Suite Teardown    shutdown SUT
Test Template    client sends POST request to /ehr

Force Tags    create_ehr    obsolete


#                                                                   | ------ EHR STATUS ----- |
*** Test Cases ***           EHR_ID      SUBJECT_ID  SUBJECT_NMSP.  IS_QUERYABLE  IS_MODIFIABLE  R.CODE
Create EHR - Data Set #01    ${EMPTY}    ${EMPTY}    ${EMPTY}       ${EMPTY}      ${EMPTY}       201
Create EHR - Data Set #02    ${EMPTY}    ${EMPTY}    ${EMPTY}       ${EMPTY}      true           201
Create EHR - Data Set #03    ${EMPTY}    ${EMPTY}    ${EMPTY}       true          ${EMPTY}       201
Create EHR - Data Set #04    ${EMPTY}    ${EMPTY}    ${EMPTY}       true          true           201
Create EHR - Data Set #05    ${EMPTY}    given111    ${EMPTY}       ${EMPTY}      true           400  # !!! given sid + empty snspace = 400 bad request
Create EHR - Data Set #06    ${EMPTY}    given112    ${EMPTY}       true          ${EMPTY}       400
Create EHR - Data Set #07    ${EMPTY}    given113    ${EMPTY}       ${EMPTY}      ${EMPTY}       400
Create EHR - Data Set #08    ${EMPTY}    given114    ${EMPTY}       true          true           400
# Create EHR - Data Set #09    ${EMPTY}    ${EMPTY}    ${EMPTY}       ${EMPTY}      false          201  # sending EHR STATUS within POST /ehr request
# Create EHR - Data Set #10    ${EMPTY}    ${EMPTY}    ${EMPTY}       true          false          201  # is not supported by EhrScape and won't be fixed
# Create EHR - Data Set #11    ${EMPTY}    given115    ${EMPTY}       ${EMPTY}      false          400  # by EtherCIS (yet)
# Create EHR - Data Set #12    ${EMPTY}    given116    ${EMPTY}       true          false          400
# Create EHR - Data Set #13    ${EMPTY}    ${EMPTY}    ${EMPTY}       false         ${EMPTY}       201
# Create EHR - Data Set #14    ${EMPTY}    ${EMPTY}    ${EMPTY}       false         true           201
# Create EHR - Data Set #15    ${EMPTY}    given117    ${EMPTY}       false         ${EMPTY}       400
# Create EHR - Data Set #16    ${EMPTY}    given118    ${EMPTY}       false         true           400
# Create EHR - Data Set #17    ${EMPTY}    ${EMPTY}    ${EMPTY}       false         false          201
# Create EHR - Data Set #18    ${EMPTY}    given119    ${EMPTY}       false         false          400
Create EHR - Data Set #19    ${EMPTY}    ${EMPTY}    given001       ${EMPTY}      ${EMPTY}       400  # !!! empty sid + given snspace = bad request
Create EHR - Data Set #20    ${EMPTY}    ${EMPTY}    given002       ${EMPTY}      true           400
Create EHR - Data Set #21    ${EMPTY}    ${EMPTY}    given003       true          ${EMPTY}       400
Create EHR - Data Set #22    ${EMPTY}    ${EMPTY}    given004       true          true           400
Create EHR - Data Set #23    ${EMPTY}    given121    given121       ${EMPTY}      true           201
Create EHR - Data Set #24    ${EMPTY}    given122    given122       true          ${EMPTY}       201
Create EHR - Data Set #25    ${EMPTY}    given123    given123       ${EMPTY}      ${EMPTY}       201
Create EHR - Data Set #26    ${EMPTY}    given124    given124       true          true           201
# Create EHR - Data Set #27    ${EMPTY}    ${EMPTY}    given005       ${EMPTY}      false          400
# Create EHR - Data Set #28    ${EMPTY}    ${EMPTY}    given006       true          false          400
# Create EHR - Data Set #29    ${EMPTY}    given125    given125       ${EMPTY}      false          201
# Create EHR - Data Set #30    ${EMPTY}    given126    given126       true          false          201
# Create EHR - Data Set #31    ${EMPTY}    ${EMPTY}    given007       false         ${EMPTY}       400
# Create EHR - Data Set #32    ${EMPTY}    ${EMPTY}    given008       false         true           400
# Create EHR - Data Set #33    ${EMPTY}    given127    given127       false         ${EMPTY}       201
# Create EHR - Data Set #34    ${EMPTY}    given128    given128       false         true           201
# Create EHR - Data Set #35    ${EMPTY}    ${EMPTY}    given009       false         false          400
# Create EHR - Data Set #36    ${EMPTY}    given129    given129       false         false          201
Create EHR - Data Set #37    ${EMPTY}    { }         { }            ${EMPTY}      ${EMPTY}       201  # TODO: propose to create unit test to validate subject_id
Create EHR - Data Set #38    ${EMPTY}    { }         { }            true          true           201  #       and subject_namespace
Create EHR - Data Set #39    ${EMPTY}    ''          ' '            ${EMPTY}      ${EMPTY}       201

# NOTE: This is supported by openEHR API but not by EhrScape - so won't be fixed by EtherCIS (yet)
#       confirmed by @Birger and @Pablo
#
# # Test Data Sets #9-16 in actual Doc
# *** Test Cases ***                   EHR_ID        SUBJECT_ID  SUBJECT_NMSP.  IS_QUERYABLE  IS_MODIFIABLE  R.CODE
# Create EHR with ID - Data Set #01    ehr-id-001    ${EMPTY}    ${EMPTY}       ${EMPTY}      ${EMPTY}       201
# Create EHR with ID - Data Set #02    ehr-id-002    ${EMPTY}    ${EMPTY}       ${EMPTY}      true           201
# Create EHR with ID - Data Set #03    ehr-id-003    ${EMPTY}    ${EMPTY}       true          ${EMPTY}       201
# Create EHR with ID - Data Set #04    ehr-id-004    ${EMPTY}    ${EMPTY}       true          true           201
# Create EHR with ID - Data Set #05    ehr-id-005    given111    ${EMPTY}       ${EMPTY}      true           400
# Create EHR with ID - Data Set #06    ehr-id-006    given112    ${EMPTY}       true          ${EMPTY}       400
# Create EHR with ID - Data Set #07    ehr-id-007    given113    ${EMPTY}       ${EMPTY}      ${EMPTY}       400
# Create EHR with ID - Data Set #08    ehr-id-008    given114    ${EMPTY}       true          true           400
# Create EHR with ID - Data Set #09    ehr-id-009    ${EMPTY}    ${EMPTY}       ${EMPTY}      false          201
# Create EHR with ID - Data Set #10    ehr-id-010    ${EMPTY}    ${EMPTY}       true          false          201
# Create EHR with ID - Data Set #11    ehr-id-011    given115    ${EMPTY}       ${EMPTY}      false          400
# Create EHR with ID - Data Set #12    ehr-id-012    given116    ${EMPTY}       true          false          400
# Create EHR with ID - Data Set #13    ehr-id-013    ${EMPTY}    ${EMPTY}       false         ${EMPTY}       201
# Create EHR with ID - Data Set #14    ehr-id-014    ${EMPTY}    ${EMPTY}       false         true           201
# Create EHR with ID - Data Set #15    ehr-id-015    given117    ${EMPTY}       false         ${EMPTY}       400
# Create EHR with ID - Data Set #16    ehr-id-016    given118    ${EMPTY}       false         true           400
# Create EHR with ID - Data Set #17    ehr-id-017    ${EMPTY}    ${EMPTY}       false         false          201
# Create EHR with ID - Data Set #18    ehr-id-018    given119    ${EMPTY}       false         false          400
# Create EHR with ID - Data Set #19    ehr-id-019    ${EMPTY}    given001       ${EMPTY}      ${EMPTY}       400
# Create EHR with ID - Data Set #20    ehr-id-020    ${EMPTY}    given002       ${EMPTY}      true           400
# Create EHR with ID - Data Set #21    ehr-id-021    ${EMPTY}    given003       true          ${EMPTY}       400
# Create EHR with ID - Data Set #22    ehr-id-022    ${EMPTY}    given004       true          true           400
# Create EHR with ID - Data Set #23    ehr-id-023    given121    given121       ${EMPTY}      true           201
# Create EHR with ID - Data Set #24    ehr-id-024    given122    given122       true          ${EMPTY}       201
# Create EHR with ID - Data Set #25    ehr-id-025    given123    given123       ${EMPTY}      ${EMPTY}       201
# Create EHR with ID - Data Set #26    ehr-id-026    given124    given124       true          true           201
# Create EHR with ID - Data Set #27    ehr-id-027    ${EMPTY}    given005       ${EMPTY}      false          400
# Create EHR with ID - Data Set #28    ehr-id-028    ${EMPTY}    given006       true          false          400
# Create EHR with ID - Data Set #29    ehr-id-029    given125    given125       ${EMPTY}      false          201
# Create EHR with ID - Data Set #30    ehr-id-030    given126    given126       true          false          201
# Create EHR with ID - Data Set #31    ehr-id-031    ${EMPTY}    given007       false         ${EMPTY}       400
# Create EHR with ID - Data Set #32    ehr-id-032    ${EMPTY}    given008       false         true           400
# Create EHR with ID - Data Set #33    ehr-id-033    given127    given127       false         ${EMPTY}       201
# Create EHR with ID - Data Set #34    ehr-id-034    given128    given128       false         true           201
# Create EHR with ID - Data Set #35    ehr-id-035    ${EMPTY}    given009       false         false          400
# Create EHR with ID - Data Set #36    ehr-id-036    given129    given129       false         false          201


Retrieve EHR and Check Consistence with Test Data Set
    [Template]  client sends GET request to /ehr{?subject_id,subject_namespace}
    [Tags]

  # SUBJECT_ID  SUBJECT_NMSP.  IS_QUERYABLE  IS_MODIFIABLE  R.CODE
    given121    given121       ${EMPTY}      true           200
    given122    given122       true          ${EMPTY}       200
    given123    given123       ${EMPTY}      ${EMPTY}       200
    given124    given124       true          true           200




*** Keywords ***
# startup SUT
#     get application version
#     unzip file_repo_content.zip
#     start ehrdb
#     start openehr server

client sends POST request to /ehr
    [Arguments]  ${ehr_id}  ${subject_id}  ${subject_namespace}  ${is_queryable}  ${is_modifiable}  ${status_code}

    # STEP 1 - Invoke the create EHR service (for each item in the Data set)
    build body    ${is_queryable}    ${is_modifiable}
    # &{R}=    Run Keyword If  "${subject_id}"=="" and "${subject_namespace}"==""  REST.POST  /ehr  body=${body}
    &{R}=    Run Keyword If  "${subject_id}"=="" and "${subject_namespace}"==""  create ehr without query params  body=${body}
    ...    ELSE IF  "${subject_id}"==""  create ehr with query params  subjectNamespace\=${subject_namespace}  body=${body}
    ...    ELSE IF  "${subject_namespace}"==""  create ehr with query params  subjectId\=${subject_id}  body=${body}
    ...    ELSE IF  "${ehr_id}"!="" and "${subject_id}"=="" and "${subject_namespace}"==""  REST.PUT  /ehr/${ehr_id}  body=${body}
    ...    ELSE IF  "${ehr_id}"!="" and "${subject_id}"==""  REST.PUT  /ehr/${ehr_id}?subjectNamespace=${subject_namespace}  body=${body}
    ...    ELSE IF  "${ehr_id}"!="" and "${subject_namespace}"==""  REST.PUT  /ehr/${ehr_id}?subjectId=${subject_id}  body=${body}
    ...    ELSE IF  "${ehr_id}"!="" and "${subject_id}"!="" and "${subject_namespace}"!=""  REST.PUT  /ehr/${ehr_id}?subjectId=${subject_id}&subjectNamespace=${subject_namespace}  body=${body}
    ...    ELSE  REST.POST  /ehr?subjectId=${subject_id}&subjectNamespace=${subject_namespace}  body=${body}
    Set Test Variable    ${response}    ${R}

    # STEP 2 - The server should answer with a positive response associated to "EHR created"
    Integer   response status    ${status_code}

    # STEP 3 - Postcondition: New EHR will be consistent with the data sets used
    Run Keyword And Continue On Failure  verify subject_id  ${subject_id}
    Run Keyword And Continue On Failure  verify subject_namespace  ${subject_namespace}
    Run Keyword And Continue On Failure  verify response action  CREATE
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





############### BACKUP KEYWORDS

# client sends POST request to /ehr
#     [Arguments]    ${subject_id}  ${subject_namespace}  ${status_code}
#
#     # STEP 1
#     ${subid} =    process arguments    ${subject_id}
#
#     # STEP 2
#     &{r}=    REST.POST    /ehr?subjectId=${subid}&subjectNamespace=${subject_namespace}
#
#     # STEP 3
#     Integer   response status    ${status_code}
#
# client sends valid POST request to ${endpoint}
#     ${subject_id}=    generate valid subject_id
#     &{r}=   REST.POST    ${endpoint}?subjectId=$${subject_id}&subjectNamespace=snamespace_${subject_id}
#     Set Test Variable    ${response}    ${r}
#     Set Test Variable    ${sid}  ${response.body.ehrStatus.subjectId}
#     Set Test Variable    ${sns}  ${response.body.ehrStatus.subjectNamespace}

# new EHR record is created
#     &{r}=    REST.GET    /ehr?subjectId=${sid}&subjectNamespace=${sns}
#     Integer    response status    201  201  202  208  400
#     Set Test Variable    ${response}    ${r}
#
# generate valid subject_id
#     ${s}=    Generate Random String    3    [LOWER]
#     ${i}=    Generate Random String    4    [NUMBERS]
#     ${subject_id}=	   Set Variable    ${s}${i}
#     [RETURN]    ${subject_id}
#
# generate invalid subject_id
#     ${s}=    Set Variable    %%%
#     ${i}=    Set Variable    123
#     ${subject_id}=	   Set Variable    ${s}${i}
#     [RETURN]    ${subject_id}



# ############### BACKUP TEST CASES
# *** Test Cases ***
# Create ehr with auto-generated ID
#     [Documentation]    POST  /ehr
#     ...                 Create a new EHR
#     create ehr    1234-111    namespace_111
#     verify subject_id  1234-111
#     verify subject_namespace  namespace_111
#     verify response action  CREATE
#     expect response status  201
#
# Create identical EHR twice
#     create ehr  1234-222  namespace_222
#     create ehr  1234-222  namespace_222
#     expect response status  400
#
# Create ehr with missing subjectId
#     create ehr    ${EMPTY}    namespace_100
#     expect response status  400
#
# Create ehr with missing subjectNamespace
#     [Tags]    not-ready
#     Log a WARNING  status code should be 400 - Bad Request!
#     create ehr    1234-333    ${EMPTY}
#     expect response status  400
#
# Get EHR summary by subject and namespace
#     [Documentation]    GET /ehr
#     ...                 ?subjectId&subjectNamespace
#     ...                 Returns information about EHR specified by subject ID
#     ...                 and namespace.
#     create ehr  1234-444  namespace_444
#     get ehr by subject-id and namespace  1234-444  namespace_444
#     expect response status  201
#
# Get EHR summary by ID
#     [Documentation]    Returns information about the specified EHR.
#     ...
#     create ehr  1234-114  namespace_201
#     extract ehrId
#     get ehr by id  ${ehr_id}
#     expect response status  201
#
# Get EHR summary by non-existing ID
#     get ehr by id  1422ba6e-8ff7-4caa-be33-b5784fd96368
#     expect response status  404
#
# Get EHR summary by invalid ID
#     [Tags]   not-ready
#     Log a WARNING    status code should be 400 - Bad Request!
#     get ehr by id  ${invalid_ehr_id}
#     expect response status  400
#
#
# Update EHR status
#     create ehr  119  namespace_111
#     extract ehrId
#     update ehr  ${ehr_id}
#     verify subject_id  333-333
#     verify subject_namespace  Graf Drakula
