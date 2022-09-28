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
Documentation   QUERY Tests from Tests Set 3
...             \nFirst test Setup is creating OPT, EHR and compositions for Tests Set 3
...             \n*POSTCONDITION* Delete all rows from composition, ehr, status, status_history, template_store

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/admin_keywords.robot
Resource        ../_resources/suite_settings.robot
Resource        ../_resources/keywords/aql_query_keywords.robot

Suite Setup         Clean DB
Suite Teardown      Clean DB


*** Variables ***
${optFile}      ehrbase.testcase06.v0.opt
${testSet}      test_set_3


*** Test Cases ***
Query For COMPOSITION[x] > OBSERVATION[x] > CLUSTER[x]
    [Tags]      not-ready   bug
    [Setup]     Prepare Test Set 3 From Query Execution
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_observation_x_cluster_x.json
    ...     q_for_composition_x_observation_x_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For OBSERVATION > CLUSTER[x]
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_cluster_x.json
    ...     q_for_observation_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For OBSERVATION[x] > CLUSTER
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_x_cluster.json
    ...     q_for_observation_x_cluster.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For OBSERVATION[x] > CLUSTER[x] BP Device
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_x_cluster_x_bp_device.json
    ...     q_for_observation_x_cluster_x_bp_device.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For ACTION[x] > CLUSTER[x]
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_action_x_cluster_x.json
    ...     q_for_action_x_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For EVALUATION[x] > CLUSTER[x]
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_evaluation_x_cluster_x.json
    ...     q_for_evaluation_x_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For OBSERVATION[x] > CLUSTER[x]
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_x_cluster_x.json
    ...     q_for_observation_x_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For INSTRUCTION[x] > CLUSTER[x]
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_instruction_x_cluster_x.json
    ...     q_for_instruction_x_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For ADMIN[x] > CLUSTER[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_admin_x_cluster_x.json
    ...     q_for_admin_x_cluster_x.json
    ...     test_set=${testSet}

Query For COMPOSITION > OBSERVATION[x] > CLUSTER[x] > CLUSTER[x]
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_observation_x_cluster_x_cluster_x.json
    ...     q_for_composition_observation_x_cluster_x_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For ( COMP > EVAL[x] ) And ( COMP > INST[x] )
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_evaluation_x_and_composition_instruction_x.json
    ...     q_for_composition_evaluation_x_and_composition_instruction_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For COMP > ( OBS[x] And OBS[x] )
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_observation_x_and_observation_x.json
    ...     q_for_composition_observation_x_and_observation_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556

Query For OBS[x] Or OBS[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_x_or_observation_x.json
    ...     q_for_observation_x_or_observation_x.json
    ...     test_set=${testSet}

Query For COMP[x] Or OBS[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_or_observation_x.json
    ...     q_for_composition_x_or_observation_x.json
    ...     test_set=${testSet}

Query For OBS[x] Or ACTION[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_x_or_action_x.json
    ...     q_for_observation_x_or_action_x.json
    ...     test_set=${testSet}

Query For ACTION[x] Or INST[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_action_x_or_instruction_x.json
    ...     q_for_action_x_or_instruction_x.json
    ...     test_set=${testSet}

Query For ( COMP[x] > ACTION ) Or ( COMP[x] > OBS )
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_action_or_composition_x_observation.json
    ...     q_for_composition_x_action_or_composition_x_observation.json
    ...     test_set=${testSet}

Query For OBS > ( CLUSTER[x] And CLUSTER[x] )
    [Tags]      not-ready   bug
    Execute Query And Compare Actual Result With Expected
    ...     q_for_observation_cluster_x_and_cluster_x.json
    ...     q_for_observation_cluster_x_and_cluster_x.json
    ...     test_set=${testSet}
    [Teardown]      TRACE JIRA ISSUE    CDR-556


*** Keywords ***
Prepare Test Set 3 From Query Execution
    ${opt_file_name}    Set Variable    ehrbase.testcase06.v0
    Upload OPT    query_test_sets/${optFile}
    prepare new request session    JSON    Prefer=return=representation
    create new EHR with subject_id and default subject id value (JSON)
    check content of created EHR (JSON)
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo1_test_set_3.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo2_test_set_3.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo3_test_set_3.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo4_no_obs_cluster_test_set_3.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo5_no_action_cluster_test_set_3.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo6_no_evaluation_cluster_test_set_3.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo7_no_instruction_cluster_test_set_3.json