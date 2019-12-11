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
...             validate invalid OPTs
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
...                 For each invalid OPT in the data set, invoke the OPT validation service
...                 The result should be negative related to the "OPT is invalid",
...                 would be useful if the server also returns where the problems are in the OPT

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14    future



*** Test Cases ***

Empty File
    [Documentation]    Different issues with the file content.
    [Template]         validate invalid OPT

    empty_file/empty_file.opt
    empty_file/empty_xml.opt
    empty_file/empty_xml_template.opt


Empty Template ID
    [Documentation]    Different issues with template_id.
    ...                invalid_1:
    ...                    with template_id tag but empty value
    ...                    <template_id><value></value></template_id>
    ...                invalid_2:
    ...                    with template_id tag but w/o value tag
    ...                    <template_id></template_id>
    ...                invalid_3:
    ...                    template_id tag with text
    ...                    <template_id>bullfrog</template_id>
    ...                invalid_4:
    ...                    no template_id tag at all
    [Template]         validate invalid OPT

    removed_template_id/minimal_admin_invalid_1.opt
    removed_template_id/minimal_admin_invalid_2.opt
    removed_template_id/minimal_admin_invalid_3.opt
    removed_template_id/minimal_admin_invalid_4.opt

    [Teardown]  TRACE JIRA BUG    EHR-332    not-ready


Removed Mandatory Elements
    [Documentation]    Issues with missing elements.
    ...                Mandatory, optional or combination of it.
    ...                Elements without minOccurs or MaxOccurs are mandatory
    [Template]         validate invalid OPT

    removed_mandatory_elements/minimal_action_removed_concept.opt
    removed_mandatory_elements/minimal_action_removed_concept_value.opt
    removed_mandatory_elements/minimal_action_removed_definition.opt

    # SPECIAL: removing an optional AND a mandatory element
    removed_mandatory_elements/minimal_action_removed_description_and_concept.opt

    [Teardown]  TRACE JIRA BUG    EHR-333    not-ready


Multiple Elements With Upper Bound Of 1
    [Documentation]    Issues with elements that should occur only once.
    ...                Any element without an maxOccurs="unbounded" has upper bound=1
    ...
    [Template]         validate invalid OPT

    multiple_elements/minimal_action_template-id_twice_1.opt
    multiple_elements/minimal_action_template-id_twice_2.opt
    multiple_elements/minimal_action_template-id_twice_3.opt
    multiple_elements/minimal_action_concept_twice_1.opt
    multiple_elements/minimal_action_concept_twice_2.opt
    multiple_elements/minimal_action_definition_twice.opt

    [Teardown]  TRACE JIRA BUG    EHR-334    not-ready
                # server accepts OPTs with multiple template_id tags, etc
                # actual --> status code 201
                # expected --> status code 400


Alien Tags
    [Documentation]    Issues with unknown tags.
    ...                e.g. <bullfrog>Minimal action</bullfrog>
    [Template]         validate invalid OPT

    # NOTE: added tag <bullfrog>Minimal action</bullfrog>
    alien_tags/minimal_action.opt

    [Teardown]  TRACE JIRA BUG    EHR-335    not-ready
                # should the server responce with a 400 here?



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

validate invalid OPT
    [Arguments]             ${opt file}

    start request session
    get invalid OPT file    ${opt file}
    upload OPT file
    server's response indicates that OPT is invalid


server's response indicates that OPT is invalid
    Should Be Equal As Strings   ${response.status_code}   400
