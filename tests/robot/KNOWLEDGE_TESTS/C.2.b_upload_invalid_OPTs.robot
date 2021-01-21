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
...             upload invalid OPTs
...
...             Precondtions for exectuion:
...               1. operational_templates folder is empty
...               2. DB container started
...               3. openehr-server started
...
...             Preconditions:
...                 No OPTs should be loaded on the system.
...
...             Postconditions:
...                 No OPTs should be loaded on the system.
...
...             Flow:
...                 For each invalid OPT in the data set, invoke the OPT upload service
...                 The result should be negative, the server rejected the OPT because it was invalid,
...                 would be useful if the result contains where the errors are in the uploaded OPT.
Metadata        TOP_TEST_SUITE    EHR_STATUS
Resource        ${CURDIR}${/}../_resources/suite_settings.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14    OPT14_upload    OPT14_upload_invalid



*** Test Cases ***

Empty File
    [Documentation]    Different issues with the file content.
    [Template]         upload invalid OPT

    empty_file/empty_file.opt
    empty_file/empty_xml.opt
    empty_file/empty_xml_template.opt


Removed Mandatory Elements
    [Documentation]     Issues with missing elements.
    ...                 Mandatory, optional or combination of it.
    ...                 Elements without minOccurs or MaxOccurs are mandatory
    [Template]          upload invalid OPT
    [Tags]

    removed_mandatory_elements/minimal_action_removed_concept.opt
    removed_mandatory_elements/minimal_action_removed_concept_value.opt
    removed_mandatory_elements/minimal_action_removed_definition.opt

    # SPECIAL: removing an optional AND a mandatory element
    removed_mandatory_elements/minimal_action_removed_description_and_concept.opt


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
