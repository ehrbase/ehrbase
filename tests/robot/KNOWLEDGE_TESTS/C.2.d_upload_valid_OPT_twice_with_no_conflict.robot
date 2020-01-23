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
...             upload valid OPT twice w/o conflict
...
...             Precondtions for exectuion:
...                 1. operational_templates folder is empty
...                 2. DB container started
...                 3. openehr-server started
...
...             Preconditions:
...                 No OPTs should be loaded on the system.
...
...             Postconditions:
...                 Two new OPTs with the given template_id and different versions are loaded
...                 into the server.
...
...                 Note: When retrieving an OPT giving only the template id, the server will
...                       return just the latest version.
...
...             Flow:
...                 1. For each valid OPT in the data set, invoke the OPT upload service,
...                    including the version parameter = 1
...                 2. The result should be positive (the server accepted the OPT)
...                 3. Invoke the upload service with the same OPT as in 1.,
...                    including the version parameter = 2
...                 4. The result should be positive (the server accepted the OPT)

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
# Suite Teardown  shutdown SUT

Force Tags   OPT14    obsolete



*** Comments ***
THIS IS NOT APPLICABLE FOR ADL 1.4

SKIPPING IMPLEMENTATION FOR NOW!!! WILL BE CONTINUED AS SOON AS ADL 2.0 becomes relevant.



*** Test Cases ***

All Types
    [Documentation]    ...
    [Template]         upload valid OPT twice w/o conflict

    all_types/Test_all_types.opt


# Minimal
#     [Documentation]    ...
#     [Template]         upload valid OPT twice w/o conflict
#
#     minimal/minimal_action.opt
#     minimal/minimal_admin.opt
#     minimal/minimal_evaluation.opt
#     minimal/minimal_instruction.opt
#     minimal/minimal_observation.opt
#
# Minimal Entry Combination
#     [Documentation]    ...
#     [Template]         upload valid OPT twice w/o conflict
#
#     minimal_entry_combination/obs_act.opt
#     minimal_entry_combination/obs_admin.opt
#     minimal_entry_combination/obs_eva.opt
#     minimal_entry_combination/obs_inst.opt
#
# Minimal Persistent
#     [Documentation]    ...
#     [Template]         upload valid OPT twice w/o conflict
#
#     minimal_persistent/persistent minimal all entries.opt
#     minimal_persistent/persistent_minimal_2.opt
#     minimal_persistent/persistent_minimal.opt
#
# Nested
#     [Documentation]    ...
#     [Template]         upload valid OPT twice w/o conflict
#
#     nested/nested.opt
#
# Versioned
#     [Documentation]    ...
#     [Template]         upload valid OPT twice w/o conflict
#
#     versioned/Test versioned v1.opt
#     versioned/Test versioned v2.opt



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

upload valid OPT twice w/o conflict
    [Arguments]            ${opt file}

    Log               NOT APPLICABLE FOR ADL 1.4    level=WARN
    Pass Execution    NOT APPLICABLE FOR ADL 1.4    not-ready

    prepare new request session    XML
    ...                            Prefer=return=representation
    get valid OPT file     ${opt file}
    upload OPT file with version parameter  1    # not implemented
    server accepted OPT
    upload same OPT with version parameter  2    # not implemented
    server accepted OPT
