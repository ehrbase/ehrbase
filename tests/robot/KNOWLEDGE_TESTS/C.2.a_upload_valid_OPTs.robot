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
...             upload valid OPTs
...
...             Precondtions for exectuion:
...                 1. operational_templates folder is empty
...                 2. DB container started
...                 3. openehr-server started
...
...             Preconditions:
...                 No OPTs should be loaded on the system.
...             Postconditions:
...                 A new OPT with the given template_id is loaded into the server.
...
...             Flow:
...                 For each valid OPT in the data set, invoke the OPT upload service
...                 The result should be positive, the server accepted the OPT
...                 and it is stored as it was uploaded

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14



*** Test Cases ***

All Types
    [Documentation]    TODO: description from business perspective
    [Template]         upload valid OPT

    all_types/Test_all_types.opt


Minimal
    [Documentation]    ...
    [Template]         upload valid OPT

    minimal/minimal_action.opt
    minimal/minimal_admin.opt
    minimal/minimal_evaluation.opt
    minimal/minimal_instruction.opt
    minimal/minimal_observation.opt


Minimal Entry Combination
    [Documentation]    ...
    [Template]         upload valid OPT

    minimal_entry_combination/obs_act.opt
    minimal_entry_combination/obs_admin.opt
    minimal_entry_combination/obs_eva.opt
    minimal_entry_combination/obs_inst.opt


Minimal Persistent
    [Documentation]    ...
    [Template]         upload valid OPT

    minimal_persistent/persistent_minimal_all_entries.opt
    minimal_persistent/persistent_minimal_2.opt
    minimal_persistent/persistent_minimal.opt


Nested
    [Documentation]    ...
    [Template]         upload valid OPT

    nested/nested.opt


Versioned
    [Documentation]    ...
    [Template]         upload valid OPT

    versioned/Test versioned v1.opt
    versioned/Test versioned v2.opt



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

upload valid OPT
    [Arguments]       ${opt file}

    start request session
    get valid OPT file      ${opt file}
    extract template_id from OPT file
    upload OPT file
    server accepted OPT
    OPT is stored as it was uploaded


OPT is stored as it was uploaded
    retrieve OPT by template_id   ${template_id}
    verify content of OPT
