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
...             retrieve specific version of versioned OPT
...
...             Precondtions for exectuion:
...                 1. operational_templates folder is empty
...                 2. DB container started
...                 3. openehr-server started
...
...             Preconditions:
...                 OPTs with more than one version should be loaded
...
...             Postconditions:
...                 None
...
...             Flow:
...                 1. Invoke the retrieve OPT service with existing template_ids and
...                    a version parameter value that is not the last
...                 2. For each template_id, the correct OPT will be returned,
...                    and will be the requested version

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
# Suite Teardown  Delete All Templates

Force Tags   OPT14    future



*** Comments ***
THIS IS NOT APPLICABLE FOR ADL 1.4 - VERSIONING IS NOT SUPPORTED.

SKIPPING IMPLEMENTATION FOR NOW!!! WILL BE CONTINUED AS SOON AS ADL 2.0 becomes relevant.



*** Test Cases ***

# Establish Preconditions: load versioned OPTs into SUT
#     [Template]         upload valid OPT
#     [Documentation]    SUT == System Under Test
#
#     versioned/Test versioned v1.opt
#     versioned/Test versioned v2.opt

Retrieve Versioned OPT
    [Documentation]    ...
    [Template]         retrieve versioned OPT

    versioned/Test versioned v2.opt
