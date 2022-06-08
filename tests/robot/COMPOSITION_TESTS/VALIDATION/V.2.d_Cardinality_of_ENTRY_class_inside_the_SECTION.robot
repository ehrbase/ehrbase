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

Force Tags      282


*** Variables ***
${COMPOSITIONS_PATH}    ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_JSON

*** Test Cases ***
Cardinality of ENTRY class inside the SECTION
    [Template]    Cardinality of ENTRY class inside the SECTION with parameters
    #item name       array path          count    code    cardinality
    Evaluation #0    content[0].items    0        201     #'[0...*]'
    Evaluation #0    content[0].items    1        201
    Evaluation #0    content[0].items    3        201

    Evaluation #1    content[1].items    0        201     #'[1...1]'
    Evaluation #1    content[1].items    1        201
    Evaluation #1    content[1].items    3        422

    Evaluation #2    content[2].items    0        422     #'[1...*]'
    Evaluation #2    content[2].items    1        201
    Evaluation #2    content[2].items    3        201

    Evaluation #3    content[3].items    0        422     #'[1...1]'
    Evaluation #3    content[3].items    1        201
    Evaluation #3    content[3].items    3        422

    Evaluation #4    content[4].items    0        422     #'[3...*]'
    Evaluation #4    content[4].items    1        422
    Evaluation #4    content[4].items    3        201
    
    Evaluation #5    content[5].items    0        422     #'[3...5]'
    Evaluation #5    content[5].items    1        422
    Evaluation #5    content[5].items    3        201

    [Teardown]    TRACE GITHUB ISSUE    282



*** Keywords ***
Cardinality of ENTRY class inside the SECTION with parameters
    [Arguments]    ${item_name}    ${array_path}    ${expected_count}   ${status_code}

    ${file}=    Get File    ${COMPOSITIONS_PATH}/composition_evaluation_test__full.json
    ${file}=    Make Expected Count Items By Name      json_str=${file}
    ...                                                array_path=${array_path}
    ...                                                item_name=${item_name}
    ...                                                expected_count=${expected_count}

    Create File    ${COMPOSITIONS_PATH}/composition_evaluation_test__full_temp.json    ${file}

    commit composition    format=CANONICAL_JSON
    ...    composition=composition_evaluation_test__full_temp.json
    check status_code of commit composition    ${status_code}

Precondition
    Upload OPT    validation/composition_evaluation_test.opt
    create EHR

Postcondition
    Remove File    ${COMPOSITIONS_PATH}/composition_evaluation_test__full_temp.json
    #restart SUT
