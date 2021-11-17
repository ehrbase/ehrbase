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
Documentation     Composition Integration Tests
Metadata          TOP_TEST_SUITE    COMPOSITION
Resource        ../../_resources/keywords/composition_keywords.robot

Suite Setup       Precondition
Suite Teardown    Postcondition

Force Tags      282    not-ready    bug


*** Variables ***
${COMPOSITIONS_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_JSON

*** Test Cases ***
Cardinality of SECTION class
    [Template]    Cardinality of SECTION class with parameters    
    #item name                    count      code       cardinality
    Validation section test #0    0          201        #'[0...*]'
    Validation section test #0    1          201
    Validation section test #0    3          201

    Validation section test #1    0          201        #'[0...1]'
    Validation section test #1    1          201
    Validation section test #1    3          422

    Validation section test #2    0          422        #'[1...*]'
    Validation section test #2    1          201
    Validation section test #2    3          201

    Validation section test #3    0          422        #'[1...1]'
    Validation section test #3    1          201
    Validation section test #3    3          422

    Validation section test #4    0          422        #'[3...*]'
    Validation section test #4    1          422
    Validation section test #4    3          201
    
    Validation section test #5    0          422        #'[3...5]'
    Validation section test #5    1          422
    Validation section test #5    3          201

*** Keywords ***
Cardinality of SECTION class with parameters
    [Arguments]    ${item_name}    ${expected_count}   ${status_code}

    ${file}=    Get File    ${COMPOSITIONS_PATH}/cardinality_of_section__full.json
    ${file}=    Make Expected Count Items By Name      json_str=${file}
    ...                                                array_path=content
    ...                                                item_name=${item_name}
    ...                                                expected_count=${expected_count}

    Create File    ${COMPOSITIONS_PATH}/cardinality_of_section__full_temp.json    ${file}

    commit composition    format=CANONICAL_JSON
    ...    composition=cardinality_of_section__full_temp.json
    check status_code of commit composition    ${status_code}

Precondition
    upload OPT    validation/cardinality_of_section.opt
    create EHR

Postcondition
    Remove File    ${COMPOSITIONS_PATH}/cardinality_of_section__full_temp.json
    restart SUT
