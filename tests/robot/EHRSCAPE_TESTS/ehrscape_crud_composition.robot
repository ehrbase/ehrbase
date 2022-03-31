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
Documentation   EHRScape Composition Test. CRUD operations on Composition (Create, Update, Delete, Get)
...             Documentation URL to be defined

Resource        ../_resources/keywords/composition_keywords.robot

Suite Teardown      restart SUT


*** Test Cases ***
Main flow create and update Composition
    [Tags]
    Create Template     all_types/ehrn_family_history.opt
    Extract Template_id From OPT File
    get web template by template id     ${template_id}
    create EHR
    ${externalTemplate}     Set Variable    ${template_id}
    Set Test Variable       ${externalTemplate}
    ## Create action
    commit composition      format=FLAT
    ...                     composition=ehrn_family_history__.json
    ...                     extTemplateId=true
    check the successful result of commit composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    ## Update action
    Update Composition (FLAT)   new_version_of_composition=ehrn_family_history.v2__.json
    ## Get action
    (FLAT) get composition by composition_uid    ${composition_uid}
    check composition exists


Main flow create and delete Composition
    [Tags]
    Create Template     all_types/family_history.opt
    Extract Template_id From OPT File
    get web template by template id     ${template_id}
    create EHR
    ${externalTemplate}     Set Variable    ${template_id}
    Set Test Variable       ${externalTemplate}
    commit composition      format=FLAT
    ...                     composition=family_history__.json
    ...                     extTemplateId=true
    check the successful result of commit composition
    (FLAT) get composition by composition_uid    ${composition_uid}
    ## Delete action
    delete composition    ${composition_uid}    ehrScape=true
    get deleted composition (EHRScape)
    [Teardown]      TRACE JIRA ISSUE    CDR-324


*** Keywords ***
Create Template
    [Arguments]     ${fileLocation}
    upload OPT ECIS     ${fileLocation}