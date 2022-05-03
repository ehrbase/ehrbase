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
# Author: Vladislav Ploaia


*** Settings ***
Documentation       EHRScape Tests
...                 Documentation URL to be defined

Resource            ../_resources/keywords/composition_keywords.robot

#Suite Setup    Precondition
Suite Teardown      restart SUT


*** Test Cases ***
Main flow create Template and GET by Template ID
    Upload OPT ECIS    all_types/ehrn_family_history.opt
    Extract Template Id From OPT File
    Get Web Template By Template Id (ECIS)    ${template_id}

Main flow create and GET all Templates
    Upload OPT ECIS    all_types/family_history.opt
    Status Should Be    200
    Extract Template Id From OPT File
    ${template1}    Set Variable    ${template_id}
    Upload OPT ECIS    minimal/minimal_observation.opt
    Status Should Be    200
    Extract Template Id From OPT File
    ${template2}    Set Variable    ${template_id}
    Get All Web Templates
    Check If Get Templates Response Has
    ...    ${template1}    ${template2}
