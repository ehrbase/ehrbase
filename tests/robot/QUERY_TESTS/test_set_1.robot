# Copyright (c) 2022 Vladislav Ploaia (Vitagroup - CDR Core Team)
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
Documentation   QUERY Tests from Tests Set 1

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/admin_keywords.robot
Resource        ../_resources/suite_settings.robot
Resource        ../_resources/keywords/aql_query_keywords.robot

#Suite Teardown  Cleanup Db


*** Variables ***
${optFile}      ehrbase.testcase05.v0.opt
${testSet}      test_set_1
${ACTUAL JSON TO SEND PAYLOAD}   ${CURDIR}${/}../_resources/test_data_sets/query/aql_queries_valid/${testSet}
${EXPECTED JSON TO RECEIVE PAYLOAD}   ${CURDIR}${/}../_resources/test_data_sets/query/expected_results/${testSet}

*** Test Cases ***
Get EHRs Using Query
    #[Setup]     Prepare Test Set 1 From Query Execution
    Execute Query And Compare Actual Result With Expected
    ...     q_ehrs.json
    ...     q_ehrs.json

Get Compositions
    Execute Query And Compare Actual Result With Expected
    ...     q_for_compositions.json
    ...     q_for_compositions.json

Get Observation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation.json
    ...     q_for_observation.json

Get Observation X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_x.json
    ...     q_for_observation_x.json

Get Action
    Execute Query And Compare Actual Result With Expected
    ...     q_for_action.json
    ...     q_for_action.json

Get Action X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_action_x.json
    ...     q_for_action_x.json

Get Evaluation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_evaluation.json
    ...     q_for_evaluation.json

Get Evaluation X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_evaluation_x.json
    ...     q_for_evaluation_x.json

Get Instruction
    Execute Query And Compare Actual Result With Expected
    ...     q_for_instruction.json
    ...     q_for_instruction.json

Get Instruction X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_instruction_x.json
    ...     q_for_instruction_x.json

Get Admin
    Execute Query And Compare Actual Result With Expected
    ...     q_for_admin.json
    ...     q_for_admin.json

Get Admin X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_admin_x.json
    ...     q_for_admin_x.json

Get Cluster
    Execute Query And Compare Actual Result With Expected
    ...     q_for_cluster.json
    ...     q_for_cluster.json

Get Cluster X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_cluster_x.json
    ...     q_for_cluster_x.json
    #                        compare json-string with json-file  ${actual}  ${expected}
    ###################


*** Keywords ***
Prepare Test Set 1 From Query Execution
    Upload OPT    query_test_sets/${optFile}
    create EHR
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ehrbase.testcase05.v0__compo1_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ehrbase.testcase05.v0__compo2_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ehrbase.testcase05.v0__compo3_no_obs_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ehrbase.testcase05.v0__compo4_no_action_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ehrbase.testcase05.v0__compo5_no_evaluation_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ehrbase.testcase05.v0__compo6_no_instruction_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ehrbase.testcase05.v0__compo8_no_cluster_test_set_1.json

Commit Composition FLAT And Check Response Status To Be 201
    [Arguments]     ${compositionFile}
    commit composition   format=FLAT
    ...                  composition=${compositionFile}
    Should Be Equal As Strings      ${response.status_code}     201

Execute Query And Compare Actual Result With Expected
    [Arguments]     ${query_to_post_file}    ${expected_result_file}
    ${json_to_send}         Get File
    ...     ${ACTUAL JSON TO SEND PAYLOAD}/${query_to_post_file}
    ${json_to_receive}      Get File
    ...     ${EXPECTED JSON TO RECEIVE PAYLOAD}/${expected_result_file}
    Set Test Variable    ${payload}    ${json_to_send}
    POST /query/aql (REST)     JSON
    Set Test Variable     ${actual_query_result}    ${response body}
    &{diff}=    compare jsons    ${actual_query_result}    ${json_to_receive}    ignore_order=${TRUE}
    Should Be Empty    ${diff}    msg=DIFF DETECTED!!! \nExpected != Actual on file ${expected_result_file}