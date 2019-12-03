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
...             retrieve single OPTs
...
...             Precondtions for exectuion:
...                 1. operational_templates folder is empty
...                 2. DB container started
...                 3. openehr-server started
...
...             Preconditions:
...                 All valid OPTs should be loaded into the system, only the single versioned ones.
...
...             Postconditions:
...                 None (retrieve should not change the state of the system).
...
...             Flow:
...                 1. Invoke the retrieve OPT service with existing template_ids
...                 2. For each template_id, the correct OPT will be returned
...
...                 Note: the retrieved OPT should be exactly the same as the uploaded one.

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14



*** Test Cases ***

Establish Preconditions: load valid OPTs into SUT
    [Template]         upload valid OPT
    [Documentation]    SUT == System Under Test

    all_types/Test_all_types.opt
    minimal/minimal_action.opt
    minimal/minimal_admin.opt
    minimal/minimal_evaluation.opt
    minimal/minimal_instruction.opt
    minimal/minimal_observation.opt
    minimal_entry_combination/obs_act.opt
    minimal_entry_combination/obs_admin.opt
    minimal_entry_combination/obs_eva.opt
    minimal_entry_combination/obs_inst.opt
    minimal_persistent/persistent_minimal_all_entries.opt
    minimal_persistent/persistent_minimal_2.opt
    minimal_persistent/persistent_minimal.opt
    nested/nested.opt
    versioned/Test versioned v1.opt
    versioned/Test versioned v2.opt


All Types
    [Documentation]    ...
    [Template]         retrieve single OPT

    all_types/Test_all_types.opt

Minimal
    [Documentation]    ...
    [Template]         retrieve single OPT

    minimal/minimal_action.opt
    minimal/minimal_admin.opt
    minimal/minimal_evaluation.opt
    minimal/minimal_instruction.opt
    minimal/minimal_observation.opt


Minimal Entry Combination
    [Documentation]    ...
    [Template]         retrieve single OPT

    minimal_entry_combination/obs_act.opt
    minimal_entry_combination/obs_admin.opt
    minimal_entry_combination/obs_eva.opt
    minimal_entry_combination/obs_inst.opt


Minimal Persistent
    [Documentation]    ...
    [Template]         retrieve single OPT

    minimal_persistent/persistent_minimal_all_entries.opt
    minimal_persistent/persistent_minimal_2.opt
    minimal_persistent/persistent_minimal.opt


Nested
    [Documentation]    ...
    [Template]         retrieve single OPT

    nested/nested.opt


Versioned
    [Documentation]    ...
    [Template]         retrieve single OPT

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
    [Arguments]           ${opt file}

    start request session
    get valid OPT file    ${opt file}
    upload OPT file
    server accepted OPT
    [Teardown]            clean up test variables


retrieve single OPT
    [Arguments]                         ${opt file}

    start request session
    get valid OPT file                  ${opt file}
    extract template_id from OPT file
    retrieve OPT by template_id         ${template_id}
    verify content of OPT
    [Teardown]                          clean up test variables



# verify content of OPT
#     # use this to quickly check that verification step does what it should do
#     # this line makes ${expected} an empty template xml
#     get invalid OPT file    empty_file/empty_xml_template.opt
#     Elements Should Match    ${actual}    ${expected}  normalize_whitespace=True
