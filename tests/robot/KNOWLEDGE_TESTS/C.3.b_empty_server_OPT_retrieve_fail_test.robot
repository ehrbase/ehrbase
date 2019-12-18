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
...             Retrieve OPT from empty server
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
...                 None
...
...             Flow:
...                 1. Invoke the retrieve OPT service with a random template_id
...                 2. The service should return an error related to the non existence
...                    of the requested OPT

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14



*** Test Cases ***
Retrieve OPT from empty server
    [Documentation]    ...

    retrieve OPT by random templade_id



*** Keywords ***
retrieve OPT by random templade_id

    start request session
    generate random templade_id
    retrieve OPT by template_id         ${template_id}
    verify server response
    [Teardown]                          clean up test variables


retrieve OPT by template_id
    [Arguments]        ${template_id}
    [Documentation]    Gets OPT from server with provided template_id

    ${resp}=           Get Request          ${SUT}    /definition/template/adl1.4/${template_id}
                       ...                  headers=${headers}
                       Set Test Variable    ${response}    ${resp}


verify server response
    server response indicates non existence of requested OPT


server response indicates non existence of requested OPT
    Should Be Equal As Strings   ${response.status_code}   404
