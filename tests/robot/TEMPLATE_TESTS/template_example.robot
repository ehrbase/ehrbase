# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School),
# Nataliya Flusman (Solit Clouds), Nikita Danilin (Solit Clouds)
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
# Created date: 11 April 2022



*** Settings ***
Documentation   Examples generator for Templates
...             Documentation: To be defined

Resource        ../_resources/keywords/composition_keywords.robot

#Suite Setup       Precondition
Suite Teardown      restart SUT


*** Test Cases ***
Test Example Generator for Templates (ECIS) - FLAT
    [Tags]
    [Setup]       Upload Template using ECIS endpoint
    get example of web template by template id (ECIS)      ${template_id}      FLAT
    validate that response body is in format    FLAT

Test Example Generator for Templates (ECIS) - JSON
    [Tags]
    get example of web template by template id (ECIS)      ${template_id}      JSON
    validate that response body is in format    JSON

Test Example Generator for Templates (ECIS) - XML
    [Tags]      not-ready
    get example of web template by template id (ECIS)      ${template_id}      XML
    validate that response body is in format    XML
    [Teardown]      TRACE GITHUB ISSUE    809
    ...             Test failed due to wrong response. Not XML format (ECIS). Check previous step.

###########################################

Test Example Generator for Templates (OPENEHR) - FLAT
    [Tags]
    [Setup]     Upload Template using OPENEHR endpoint
    get example of web template by template id (OPENEHR)      ${template_id}      FLAT

Test Example Generator for Templates (OPENEHR) - JSON
    [Tags]
    get example of web template by template id (OPENEHR)      ${template_id}      JSON
    validate that response body is in format    JSON

#modify this get to have not format=XML but to have in Accept headers application/xml
Test Example Generator for Templates (OPENEHR) - XML
    [Tags]      not-ready
    get example of web template by template id (OPENEHR)      ${template_id}      XML
    validate that response body is in format    XML
    [Teardown]      TRACE GITHUB ISSUE    809
    ...             Test failed due to wrong response. Not XML format (OPENEHR). Check previous step.


*** Keywords ***
Upload Template using ECIS endpoint
    [Documentation]     Keyword used to upload Template using ECIS endpoint
    upload OPT ECIS     all_types/ehrn_family_history.opt
    Extract Template_id From OPT File

Upload Template using OPENEHR endpoint
    [Documentation]     Keyword used to upload Template using OPENEHR endpoint
    upload OPT          nested/nested.opt
    Extract Template_id From OPT File