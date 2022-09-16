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
...             \nFirst test Setup is creating OPT, EHR and compositions for Tests Set 1
...             \n*POSTCONDITION* Delete all rows from composition, ehr, status, status_history, template_store

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/admin_keywords.robot
Resource        ../_resources/suite_settings.robot
Resource        ../_resources/keywords/aql_query_keywords.robot

Suite Teardown      Clean DB


*** Variables ***
${optFile}      ehrbase.testcase05.v0.opt
${testSet}      test_set_1

*** Test Cases ***
Get EHRs Using Query
    [Setup]     Prepare Test Set 1 From Query Execution
    Execute Query And Compare Actual Result With Expected
    ...     q_ehrs.json
    ...     q_ehrs.json
    ...     test_set=${testSet}

Get Compositions
    Execute Query And Compare Actual Result With Expected
    ...     q_for_compositions.json
    ...     q_for_compositions.json
    ...     test_set=${testSet}

Get Observation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation.json
    ...     q_for_observation.json
    ...     test_set=${testSet}

Get Observation X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_x.json
    ...     q_for_observation_x.json
    ...     test_set=${testSet}

Get Action
    Execute Query And Compare Actual Result With Expected
    ...     q_for_action.json
    ...     q_for_action.json
    ...     test_set=${testSet}

Get Action X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_action_x.json
    ...     q_for_action_x.json
    ...     test_set=${testSet}

Get Evaluation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_evaluation.json
    ...     q_for_evaluation.json
    ...     test_set=${testSet}

Get Evaluation X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_evaluation_x.json
    ...     q_for_evaluation_x.json
    ...     test_set=${testSet}

Get Instruction
    Execute Query And Compare Actual Result With Expected
    ...     q_for_instruction.json
    ...     q_for_instruction.json
    ...     test_set=${testSet}

Get Instruction X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_instruction_x.json
    ...     q_for_instruction_x.json
    ...     test_set=${testSet}

Get Admin
    Execute Query And Compare Actual Result With Expected
    ...     q_for_admin.json
    ...     q_for_admin.json
    ...     test_set=${testSet}

Get Admin X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_admin_x.json
    ...     q_for_admin_x.json
    ...     test_set=${testSet}

Get Cluster
    Execute Query And Compare Actual Result With Expected
    ...     q_for_cluster.json
    ...     q_for_cluster.json
    ...     test_set=${testSet}

Get Cluster X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_cluster_x.json
    ...     q_for_cluster_x.json
    ...     test_set=${testSet}

Get Composition Observation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_observation.json
    ...     q_for_composition_observation.json
    ...     test_set=${testSet}

Get Composition X Observation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_observation.json
    ...     q_for_composition_x_observation.json
    ...     test_set=${testSet}

Get Composition X Observation X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_observation_x.json
    ...     q_for_composition_x_observation_x.json
    ...     test_set=${testSet}

Get Composition Action
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_action.json
    ...     q_for_composition_action.json
    ...     test_set=${testSet}

Get Composition X Action
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_action.json
    ...     q_for_composition_x_action.json
    ...     test_set=${testSet}

Get Composition X Action X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_action_x.json
    ...     q_for_composition_x_action_x.json
    ...     test_set=${testSet}

Get Composition Evaluation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_evaluation.json
    ...     q_for_composition_evaluation.json
    ...     test_set=${testSet}

Get Composition X Evaluation
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_evaluation.json
    ...     q_for_composition_x_evaluation.json
    ...     test_set=${testSet}

Get Composition X Evaluation X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_evaluation_x.json
    ...     q_for_composition_x_evaluation_x.json
    ...     test_set=${testSet}

Get Composition Instruction
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_instruction.json
    ...     q_for_composition_instruction.json
    ...     test_set=${testSet}

Get Composition X Instruction
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_instruction.json
    ...     q_for_composition_x_instruction.json
    ...     test_set=${testSet}

Get Composition X Instruction X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_instruction_x.json
    ...     q_for_composition_x_instruction_x.json
    ...     test_set=${testSet}

Get Composition Admin
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_admin.json
    ...     q_for_composition_admin.json
    ...     test_set=${testSet}

Get Composition X Admin
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_admin.json
    ...     q_for_composition_x_admin.json
    ...     test_set=${testSet}

Get Composition X Admin X
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_admin_x.json
    ...     q_for_composition_x_admin_x.json
    ...     test_set=${testSet}
    #                        compare json-string with json-file  ${actual}  ${expected}
    ###################


*** Keywords ***
Prepare Test Set 1 From Query Execution
    ${opt_file_name}    Set Variable    ehrbase.testcase05.v0
    Upload OPT    query_test_sets/${optFile}
    create EHR
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo1_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo2_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo3_no_obs_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo4_no_action_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo5_no_evaluation_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo6_no_instruction_test_set_1.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo8_no_cluster_test_set_1.json