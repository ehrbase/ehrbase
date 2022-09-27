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
Documentation   QUERY Tests from Tests Set 4
...             \nFirst test Setup is creating OPT, EHR and compositions for Tests Set 4
...             \n*POSTCONDITION* Delete all rows from composition, ehr, status, status_history, template_store

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/admin_keywords.robot
Resource        ../_resources/suite_settings.robot
Resource        ../_resources/keywords/aql_query_keywords.robot

Suite Setup         Clean DB
Suite Teardown      Clean DB


*** Variables ***
${optFile}      ehrbase.testcase008.v0.opt
${optFile_1}    ehrbase.testcase06.v0.opt
${testSet}      test_set_4


*** Test Cases ***
Query For ( COMP[x] > OBS[x] ) Or OBS[x]
    [Setup]     Prepare Test Set 4 From Query Execution
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_observation_x_or_observation_x.json
    ...     q_for_composition_x_observation_x_or_observation_x.json
    ...     test_set=${testSet}

Query For ( COMP[x] > OBS[x] ) And OBS[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_observation_x_and_observation_x.json
    ...     q_for_composition_x_observation_x_and_observation_x.json
    ...     test_set=${testSet}

Query For ( COMP[x] > ACTION ) Or ( COMP[x] > OBS )
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_x_action_or_composition_x_observation.json
    ...     q_for_composition_x_action_or_composition_x_observation.json
    ...     test_set=${testSet}

Query For NOT COMP[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_not_composition_x.json
    ...     q_for_not_composition_x.json
    ...     test_set=${testSet}

Query For ( NOT COMP[x] ) > OBS[x]
    Execute Query And Compare Actual Result With Expected
    ...     q_for_not_composition_x_observation_x.json
    ...     q_for_not_composition_x_observation_x.json
    ...     test_set=${testSet}

Query For ( COMP > OBS[x] ) And ( NOT COMP[x] )
    Execute Query And Compare Actual Result With Expected
    ...     q_for_composition_observation_x_and_not_composition_x.json
    ...     q_for_composition_observation_x_and_not_composition_x.json
    ...     test_set=${testSet}


*** Keywords ***
Prepare Test Set 4 From Query Execution
    ${opt_file_name}        Set Variable    ehrbase.testcase008.v0
    ${opt_file_name_1}      Set Variable    ehrbase.testcase06.v0
    Upload OPT    query_test_sets/${optFile}
    Upload OPT    query_test_sets/${optFile_1}
    prepare new request session    JSON    Prefer=return=representation
    create new EHR with subject_id and default subject id value (JSON)
    check content of created EHR (JSON)
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo_1_1_test_set_4.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo_1_2_test_set_4.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo_1_3_test_set_4.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name}__compo_1_4_no_obs_cluster_test_set_4.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name_1}__compo_2_1_test_set_4.json
    Commit Composition FLAT And Check Response Status To Be 201
    ...     ${opt_file_name_1}__compo_2_2_test_set_4.json