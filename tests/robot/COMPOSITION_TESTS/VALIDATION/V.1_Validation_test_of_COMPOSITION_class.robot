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

*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ../../_resources/keywords/composition_keywords.robot

Suite Setup     Precondition
Suite Teardown  Postcondition

Force Tags      281    666    not-ready


*** Variables ***
${COMPOSITIONS_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_JSON


*** Test Cases ***

Validation test of COMPOSITION class
    [Template]    Validate of compositions

    #language    territory    category    composer    status_code
    exist        exist        exist       exist       201      # BUG 281 TODO: remove comments when fixed
    exist        not_exist    not_exist   not_exist   422      # BUG 666
    exist        invalid      invalid     invalid     400
    not_exist    not_exist    invalid     exist       400
    not_exist    invalid      exist       invalid     400
    not_exist    exist        invalid     invalid     400
    invalid      invalid      not_exist   exist       400
    invalid      exist        not_exist   not_exist   400
    invalid      not_exist    exist       invalid     400

    [Teardown]    TRACE GITHUB ISSUE    281



*** Keywords ***
Validate of compositions
    [Arguments]    ${language}   ${territory}   ${category}   ${composer}   ${status_code}

    ${file}=   Get File    ${COMPOSITIONS_PATH}/clinical_content_validation__full.json
    ${file}=   Modify Of Composition High Level Items   json_str=${file}
    ...                                                 language=${language}
    ...                                                 territory=${territory}
    ...                                                 category=${category}
    ...                                                 composer=${composer}

    Create File    ${COMPOSITIONS_PATH}/clinical_content_validation__full_temp.json    ${file}

    commit composition   format=CANONICAL_JSON
    ...                  composition=clinical_content_validation__full_temp.json
    check status_code of commit composition    ${status_code}

Precondition
    Upload OPT    validation/clinical_content_validation.opt
    create EHR

Postcondition
    Remove File    ${COMPOSITIONS_PATH}/clinical_content_validation__full_temp.json
    #restart SUT