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
Metadata    Version    0.1.0
Metadata    Author    *Wladislaw Wagner*

Documentation    KNOWLEDGE TEST SUITE
...
...             https://docs.google.com/document/d/1wdklJROuC2UQQK6x1cjPkF1l9Na2spfEIBjRbNVK-Q0
...             https://vitasystemsgmbh.atlassian.net/wiki/spaces/ETHERCIS/pages/520912897/Knowledge+Test+Suite
...
...             OPT = operational template
...
...             Precondtions for (manual) exectuion:
...               2. DB container started
...               3. openehr-server started

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot

Suite Setup  startup SUT
Suite Teardown  shutdown SUT

Force Tags    KNOWLEDGE  DEFINITION
