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
Documentation   OPT1.4 integration tests
...             validate valid OPTs
...
...             Precondtions for exectuion:
...               1. operational_templates folder is empty
...               2. DB container started
...               3. openehr-server started
...
...             Preconditions:
...                 The server should be empty (no EHRs, no commits, no OPTs).
...
...             Postconditions:
...                 None (validation should not change the state of the system).
...
...             Flow:
...                 For each valid OPT in the data set, invoke the OPT validation service
...                 The result should be positive and the server's response should be related to "OPT is valid".

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14    future



*** Test Cases ***

All Types
    [Documentation]    TODO: description from business perspective
    [Template]         validate valid OPT

    all_types/Test_all_types.opt


Minimal
    [Documentation]    ...
    [Template]         validate valid OPT

    minimal/minimal_action.opt
    minimal/minimal_admin.opt
    minimal/minimal_evaluation.opt
    minimal/minimal_instruction.opt
    minimal/minimal_observation.opt


Minimal Entry Combination
    [Documentation]    ...
    [Template]         validate valid OPT

    minimal_entry_combination/obs_act.opt
    minimal_entry_combination/obs_admin.opt
    minimal_entry_combination/obs_eva.opt
    minimal_entry_combination/obs_inst.opt


Minimal Persistent
    [Documentation]    ...
    [Template]         validate valid OPT

    minimal_persistent/persistent_minimal_all_entries.opt
    minimal_persistent/persistent_minimal_2.opt
    minimal_persistent/persistent_minimal.opt


Nested
    [Documentation]    ...
    [Template]         validate valid OPT

    nested/nested.opt


Versioned
    [Documentation]    ...
    [Template]         validate valid OPT

    versioned/Test versioned v1.opt
    versioned/Test versioned v2.opt


Removed Optional Elements
    [Documentation]    Issues with optional elements.
    ...                Server should accept OPTs with missing optinal elements as vaild.
    [Template]         validate valid OPT

    removed_optional_elements/minimal_action_removed_language.opt



# Test Suite Self Test For Debugging
#     Log To Console    \n
#     Log To Console    ${SUT}
#     Log To Console    Create Session ${SUT} ${${SUT}.URL}
#     Log To Console    auth=${${SUT}.CREDENTIALS}
#
#     Create Session    ${SUT}   ${${SUT}.URL}
#     ...               auth=${${SUT}.CREDENTIALS}    debug=2    verify=True
#     ${resp}=          Get Request    ${SUT}    /definition/template/adl1.4
#                       Log To Console    ${resp.content}
#                       Should Be Equal As Strings    ${resp.status_code}    200



*** Keywords ***

validate valid OPT
    [Arguments]            ${opt file}

    start request session
    get valid OPT file     ${opt file}
    upload OPT file
    server's response indicates that OPT is valid


server's response indicates that OPT is valid
    Should Be Equal As Strings   ${response.status_code}   201





################################################################################
############################ BACKUP ############################################

#*** Test Cases ***
#
# No Templates On Server On Initial Start
#     [Documentation]  Precondition: no files in file_repo/... folder
#     ...            when openehr-server is (re)started
#     Get Template List
#     Validate Template List Has 0 Items
#
# Exact One Template Exists After Uploading a Template
#     [Documentation]  TODO
#     upload valid template (XML)  /all_types/Test_all_types.opt
#     Get Template List
#     Validate Template List Has 1 Items
#
# Get OPT by template_id
#    [Documentation]   In EHRSCAPE this returns web templates not OPTs.
#    [Tags]
#    ${R}=   REST.GET   /template/test_all_types.en.v1/
#    Integer    response status   200
#
# Get non-existing OPT by template_id
#     [Tags]    not-ready
#     &{resp}=    REST.GET    /template/foobar/
#     Set Test Variable    ${response}    ${resp}
#     expect response status  404
#     [Teardown]  TE @DEV Wrong Status Code - tag(s): not-ready
#
# Upload OPT
#    [Documentation]   Creates an OPT from it's canonical XML form.
#    Create Session   ethlocal   ${baseurl}
#    ${opt}   Get File   ${VALID DATA SETS}${/}all_types/Test_all_types.opt
#    &{headers}=    Create Dictionary    Content-Type=application/xml
#    ${resp}   Post Request   ethlocal   /template   data=${opt}   headers=${headers}
#    Log    ${resp.json()}
#    Run Keyword If   ${resp.status_code} != 201  Log  Wrong Status Code  WARN
#    Run Keyword If   ${resp.status_code} != 201  Set Tags  not-ready
#    Should Be Equal As Strings   ${resp.status_code}   201
#
# Get canonical OPT in XML by template_id
#    [Documentation]   In EHRSCAPE this returns an OPT in XML.
#    [Tags]   not-ready
#
#    ${R}=   REST.GET   /template/test_all_types.en.v1/opt
#    Integer    response status    200
#    [Teardown]    TW /template/template_id/opt endpoint not implemented - tag(s): not-ready
#
# Delete OPT
#     [Documentation]  TODO
#     Create Session   ethlocal   ${baseurl}
#     ${resp}   Delete Request   ethlocal   /template/test_all_types.en.v1
#     Run Keyword If   ${resp.status_code} == 405   Log   DELETE is not supported   ERROR
#     Run Keyword If   ${resp.status_code} != 200  Set Tags  not-ready
#     Should Be Equal As Strings   ${resp.status_code}   200
#
# Delete OPT REST
#     [Documentation]  TODO
#     ${R}=   REST.DELETE   /template/test_all_types.en.v1
#     Output  response body
#     Integer    response status    200
#     [Teardown]    TE @DEV DELETE method not implemented - tag(s): not-ready



# *** Keywords ***
#
# get all OPTs
#    [Documentation]   Get currently loaded OPTs.
#    ...               This is not returning OPTs but metadata of the OPTs,
#    ...               also not sure if internally this is stored as OPT
#    ...               or as the web template format used by EHRSCAPE.
#    [Tags]
#    ${resp}=    REST.GET    /template
#    # Output    response body
#    Array     response body templates    #uniqueItems=true
#    # Output Schema    $.templates[0]
#    # Output Schema    $.templates[0].templateId
#    #Array     $.templates    minItems=1
#    #Array     $.templates    uniqueItems=true
#    # ${template}  Input    ${CURDIR}${/}../_resources/fixtures/template/one_template.json
#    # Output    $.templates[0]
#    # Output    $..templateId
#    # Output    $..uid
#    # Output    $..concept
#    # Output    $..errorList
#    # Array     $.templates    contains=${template}   #TODO: getting a false positiv here
#    Set Test Variable    ${response}    ${resp}
#    expect response status    200
