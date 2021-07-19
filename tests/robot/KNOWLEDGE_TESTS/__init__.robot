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
Metadata    Version    0.2.0
Metadata    Authors    *Wladislaw Wagner*, *Pablo Pazos*

Documentation    KNOWLEDGE TEST SUITE
...
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/KNOWLEDGE.md
...
...             OPT = operational template
...
...             Precondtions for (manual) exectuion:
...               2. DB container started
...               3. openehr-server started

Resource    ../_resources/suite_settings.robot

Suite Setup  startup SUT
Suite Teardown  shutdown SUT

Force Tags    OPT
