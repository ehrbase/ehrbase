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
...             retrieve all loaded OPTs when none is loaded
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
...                 1. Invoke the retrieve OPTs service
...                 2. The service should return an empty set and should not fail.

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14



*** Test Cases ***

Retrieve OPT List From Empty Server
    [Documentation]    ...

    invoke the retrieve OPTs service
    verify server response
    clean up test variables



*** Keywords ***
invoke the retrieve OPTs service
    [Documentation]    Triggers GET endpoint to list all OPTs.

    retrieve OPT list


verify server response
    [Documentation]    Multiple verifications of the response are conducted:
    ...                - response status code is 200
    ...                - response body is a list (Array)
    ...                - the list is empty

    Integer  response status  200
    Array   response body
    verify OPT list has 0 items
